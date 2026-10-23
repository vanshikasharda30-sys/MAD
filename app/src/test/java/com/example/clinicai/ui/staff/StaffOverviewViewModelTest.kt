package com.example.clinicai.ui.staff

import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.testing.MockApi
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffOverviewViewModelTest {
    private val mock = MockApi(token = "jwt")
    private val repo = StaffRepository(mock.api, mock.gson)
    @Volatile private var upcoming: MockResponse = json(200, UPCOMING)
    private val scopesRequested = java.util.Collections.synchronizedList(mutableListOf<String?>())
    @Volatile private var now = 1_000_000L

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mock.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/dashboard/overview") -> json(200, OVERVIEW)
                    path.startsWith("/api/appointments") -> {
                        val scope = request.requestUrl?.queryParameter("scope")
                        scopesRequested += scope
                        when (scope) {
                            "today" -> json(200, TODAY, total = 7)
                            "past" -> json(200, """{"success":true,"data":[]}""", total = 0)
                            else -> upcoming
                        }
                    }
                    else -> MockResponse().setResponseCode(500)
                }
            }
        }
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        runCatching { mock.shutdown() }
    }

    private fun json(code: Int, body: String, total: Int? = null) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json")
            .setBody(if (total == null) body else body.removeSuffix("}") + ""","meta":{"total":$total}}""")

    private fun settled(vm: StaffOverviewViewModel) = runBlocking {
        withTimeout(5_000) { vm.state.first { it !is UiState.Loading } }
    }

    private fun content(vm: StaffOverviewViewModel) = (settled(vm) as UiState.Success).data

    @Test fun showsRealOverviewAndAppointmentsWithTabCounts() {
        val c = content(StaffOverviewViewModel(repo, "c1", { now }))
        assertEquals(3, c.overview.queueWaiting)
        assertEquals(3, c.count(AppointmentTab.ALL))
        assertEquals(2, c.count(AppointmentTab.SCHEDULED))
        assertEquals(1, c.count(AppointmentTab.WALK_IN))
    }

    @Test fun tabAndSearchFilterTheList() {
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        content(vm)
        vm.setTab(AppointmentTab.WALK_IN)
        assertEquals(listOf("Walk Wanda"), (vm.state.value as UiState.Success).data.visibleAppointments.map { it.patient?.name })
        vm.setTab(AppointmentTab.ALL)
        vm.setQuery("  priya ")
        assertEquals(listOf("Priya Rao"), (vm.state.value as UiState.Success).data.visibleAppointments.map { it.patient?.name })
        vm.setQuery("dr. mehta")
        assertEquals(2, (vm.state.value as UiState.Success).data.visibleAppointments.size)
    }

    @Test fun missingAppointmentsRouteKeepsTheRealOverviewAndExplainsWhy() {
        upcoming = MockResponse().setResponseCode(405).setHeader("Content-Type", "text/html").setBody("<html></html>")
        val c = content(StaffOverviewViewModel(repo, "c1", { now }))
        assertEquals(3, c.overview.queueWaiting)
        assertTrue(c.appointmentsError is AppError.EndpointMissing)
        assertTrue(c.appointments.isEmpty())
    }

    @Test fun overviewFailureIsAnErrorStateAndRetryReloads() {
        mock.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                json(500, """{"success":false,"error":{"code":"INTERNAL","message":"down"}}""")
        }
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        assertTrue(settled(vm) is UiState.Error)

        setUp()
        vm.load()
        assertTrue(settled(vm) is UiState.Success)
    }

    @Test fun aSecondLoadWhileOneIsRunningIsIgnored() {
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        vm.load(); vm.load(); vm.load()
        settled(vm)
        // One initial load = one overview request and one appointments request, however often load() was called.
        assertEquals(2, mock.server.requestCount)
    }

    @Test fun upcomingIsRequestedFirstWithTheServersScopeName() {
        content(StaffOverviewViewModel(repo, "c1"))
        assertEquals(listOf<String?>("upcoming"), scopesRequested.toList())
    }

    @Test fun switchingScopeLoadsThatScopeOnlyAndKeepsTheOverview() {
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        content(vm)
        vm.setScope(com.example.clinicai.data.repository.AppointmentScope.TODAY)
        val c = runBlocking { withTimeout(5_000) { vm.state.first { it is UiState.Success && !(it.data.appointmentsLoading) && it.data.scope == com.example.clinicai.data.repository.AppointmentScope.TODAY } } }
            .let { (it as UiState.Success).data }
        assertEquals(listOf<String?>("upcoming", "today"), scopesRequested.toList())
        assertEquals(3, c.overview.queueWaiting)
        assertEquals(1, c.appointments.size)
        assertEquals("the server's total is kept when it capped the list", 7, c.total)
    }

    @Test fun anEmptyScopeIsAConfirmedEmptyListWithNoError() {
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        content(vm)
        vm.setScope(com.example.clinicai.data.repository.AppointmentScope.PAST)
        val c = runBlocking { withTimeout(5_000) { vm.state.first { it is UiState.Success && !it.data.appointmentsLoading && it.data.scope == com.example.clinicai.data.repository.AppointmentScope.PAST } } }
            .let { (it as UiState.Success).data }
        assertTrue(c.appointments.isEmpty())
        assertEquals(null, c.appointmentsError)
        assertEquals(0, c.total)
    }

    @Test fun aFailedAppointmentRequestIsAnErrorNeverAnEmptyList() {
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        content(vm)
        upcoming = json(500, """{"success":false,"error":{"code":"INTERNAL","message":"boom"}}""")
        vm.refreshAppointments()
        val c = runBlocking { withTimeout(5_000) { vm.state.first { it is UiState.Success && !it.data.appointmentsLoading && it.data.appointmentsError != null } } }
            .let { (it as UiState.Success).data }
        assertTrue(c.appointmentsError is AppError.Server)
        assertEquals(3, c.overview.queueWaiting)
    }

    @Test fun comingBackAfterTheDataGoesStaleRefetchesOnlyTheAppointments() {
        val vm = StaffOverviewViewModel(repo, "c1", { now })
        content(vm)
        vm.refreshIfStale()
        assertEquals("fresh data is not refetched", 1, scopesRequested.size)
        now += 31_000
        vm.refreshIfStale()
        runBlocking { withTimeout(5_000) { while (scopesRequested.size < 2) kotlinx.coroutines.delay(10) } }
        assertEquals(listOf<String?>("upcoming", "upcoming"), scopesRequested.toList())
        assertEquals("initial overview + appointments, then appointments only", 3, mock.server.requestCount)
    }

    private companion object {
        const val OVERVIEW = """{"success":true,"data":{"clinic":{"id":"c1","name":"Demo Clinic","area":"A","city":"C"},
            "totalDoctorCount":2,"activeDoctorCount":2,"activeDoctors":[],"queueWaiting":3,"avgWaitMinutes":15}}"""
        const val TODAY = """{"success":true,"data":[
            {"id":"t1","doctorId":"d1","type":"SCHEDULED","status":"completed","date":"2026-10-23T00:00:00.000Z","startTime":"09:00",
             "patient":{"name":"Today Tara"},"doctor":{"id":"d1","name":"Dr. Mehta"}}]}"""
        const val UPCOMING = """{"success":true,"data":[
            {"id":"a1","doctorId":"d1","type":"SCHEDULED","status":"confirmed","date":"2026-10-26T00:00:00.000Z","startTime":"10:00",
             "patient":{"name":"Priya Rao"},"doctor":{"id":"d1","name":"Dr. Mehta"}},
            {"id":"a2","doctorId":"d1","type":"SCHEDULED","status":"confirmed","date":"2026-10-26T00:00:00.000Z","startTime":"10:30",
             "patient":{"name":"Sam Khan"},"doctor":{"id":"d1","name":"Dr. Mehta"}},
            {"id":"a3","doctorId":"d2","type":"WALK_IN","status":"confirmed","date":"2026-10-26T00:00:00.000Z",
             "patient":{"name":"Walk Wanda"},"doctor":{"id":"d2","name":"Dr. Ito"}}]}"""
    }
}
