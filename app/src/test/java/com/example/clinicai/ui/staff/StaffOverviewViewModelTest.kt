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

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mock.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/dashboard/overview") -> json(200, OVERVIEW)
                    path.startsWith("/api/appointments") -> upcoming
                    else -> MockResponse().setResponseCode(500)
                }
            }
        }
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        runCatching { mock.shutdown() }
    }

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun settled(vm: StaffOverviewViewModel) = runBlocking {
        withTimeout(5_000) { vm.state.first { it !is UiState.Loading } }
    }

    private fun content(vm: StaffOverviewViewModel) = (settled(vm) as UiState.Success).data

    @Test fun showsRealOverviewAndAppointmentsWithTabCounts() {
        val c = content(StaffOverviewViewModel(repo, "c1"))
        assertEquals(3, c.overview.queueWaiting)
        assertEquals(3, c.count(AppointmentTab.ALL))
        assertEquals(2, c.count(AppointmentTab.SCHEDULED))
        assertEquals(1, c.count(AppointmentTab.WALK_IN))
    }

    @Test fun tabAndSearchFilterTheList() {
        val vm = StaffOverviewViewModel(repo, "c1")
        content(vm)
        vm.setTab(AppointmentTab.WALK_IN)
        assertEquals(listOf("Walk Wanda"), (vm.state.value as UiState.Success).data.visibleUpcoming.map { it.patient?.name })
        vm.setTab(AppointmentTab.ALL)
        vm.setQuery("  priya ")
        assertEquals(listOf("Priya Rao"), (vm.state.value as UiState.Success).data.visibleUpcoming.map { it.patient?.name })
        vm.setQuery("dr. mehta")
        assertEquals(2, (vm.state.value as UiState.Success).data.visibleUpcoming.size)
    }

    @Test fun missingAppointmentsRouteKeepsTheRealOverviewAndExplainsWhy() {
        upcoming = MockResponse().setResponseCode(405).setHeader("Content-Type", "text/html").setBody("<html></html>")
        val c = content(StaffOverviewViewModel(repo, "c1"))
        assertEquals(3, c.overview.queueWaiting)
        assertTrue(c.upcomingError is AppError.EndpointMissing)
        assertTrue(c.upcoming.isEmpty())
    }

    @Test fun overviewFailureIsAnErrorStateAndRetryReloads() {
        mock.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                json(500, """{"success":false,"error":{"code":"INTERNAL","message":"down"}}""")
        }
        val vm = StaffOverviewViewModel(repo, "c1")
        assertTrue(settled(vm) is UiState.Error)

        setUp()
        vm.load()
        assertTrue(settled(vm) is UiState.Success)
    }

    @Test fun aSecondLoadWhileOneIsRunningIsIgnored() {
        val vm = StaffOverviewViewModel(repo, "c1")
        vm.load(); vm.load(); vm.load()
        settled(vm)
        // One initial load = one overview request and one appointments request, however often load() was called.
        assertEquals(2, mock.server.requestCount)
    }

    private companion object {
        const val OVERVIEW = """{"success":true,"data":{"clinic":{"id":"c1","name":"Demo Clinic","area":"A","city":"C"},
            "totalDoctorCount":2,"activeDoctorCount":2,"activeDoctors":[],"queueWaiting":3,"avgWaitMinutes":15}}"""
        const val UPCOMING = """{"success":true,"data":[
            {"id":"a1","doctorId":"d1","type":"SCHEDULED","status":"confirmed","date":"2026-10-26T00:00:00.000Z","startTime":"10:00",
             "patient":{"name":"Priya Rao"},"doctor":{"id":"d1","name":"Dr. Mehta"}},
            {"id":"a2","doctorId":"d1","type":"SCHEDULED","status":"confirmed","date":"2026-10-26T00:00:00.000Z","startTime":"10:30",
             "patient":{"name":"Sam Khan"},"doctor":{"id":"d1","name":"Dr. Mehta"}},
            {"id":"a3","doctorId":"d2","type":"WALK_IN","status":"confirmed","date":"2026-10-26T00:00:00.000Z",
             "patient":{"name":"Walk Wanda"},"doctor":{"id":"d2","name":"Dr. Ito"}}]}"""
    }
}
