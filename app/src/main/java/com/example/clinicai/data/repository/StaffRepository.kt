package com.example.clinicai.data.repository

import com.example.clinicai.data.model.AnalyticsData
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.CreateDoctorRequest
import com.example.clinicai.data.model.DashboardClinic
import com.example.clinicai.data.model.DashboardOverview
import com.example.clinicai.data.model.RosterDoctor
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.InviteStaffRequest
import com.example.clinicai.data.model.StaffMember
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.remote.map
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Which slice of the clinic's appointments the staff dashboard shows. Values are the server's `scope` names. */
enum class AppointmentScope(val apiValue: String) { TODAY("today"), UPCOMING("upcoming"), PAST("past") }

data class AppointmentPage(val items: List<Appointment>, val total: Int)

class StaffRepository(private val api: ApiService, private val gson: Gson) {

    /**
     * The dashboard overview for [clinicId]. Uses the dedicated `GET /api/dashboard/overview` route; if the
     * server doesn't have it (an older deployment answers an HTML 404), the same figures are assembled from
     * routes every deployment already has, so the screen shows real data either way:
     *
     *  - clinic name/location: `GET /api/clinics/me` (staff-only, server-scoped to the caller's clinic)
     *  - doctor roster:        `GET /api/doctors/search?clinicId=…`
     *  - people waiting today: `GET /api/queue/{doctorId}/status` per doctor
     *
     * Nothing is estimated: if any of those requests fails the whole call fails with its real error
     * instead of showing a partial number as if it were complete.
     */
    suspend fun overview(clinicId: String): ApiResult<DashboardOverview> {
        val direct = safeApiCall(gson) { api.getDashboardOverview() }
        if (!(direct is ApiResult.Failure && direct.error is AppError.EndpointMissing)) return direct
        return composeOverview(clinicId)
    }

    private suspend fun composeOverview(clinicId: String): ApiResult<DashboardOverview> = coroutineScope {
        val clinicRequest = async { safeApiCall(gson) { api.getMyClinic() } }
        val doctorsRequest = async { safeApiCall(gson) { api.searchDoctors(clinicId = clinicId, sortBy = "avgRating", pageSize = 50) } }
        val clinic = clinicRequest.await()
        val doctors = doctorsRequest.await()
        if (clinic is ApiResult.Failure) return@coroutineScope clinic
        if (doctors is ApiResult.Failure) return@coroutineScope doctors
        clinic as ApiResult.Success
        doctors as ApiResult.Success

        val roster = doctors.data.sortedBy { it.name }
        val queues = roster.map { doctor -> async { safeApiCall(gson) { api.getQueueStatus(doctor.id) } } }.awaitAll()
        queues.filterIsInstance<ApiResult.Failure>().firstOrNull()?.let { return@coroutineScope it }

        ApiResult.Success(
            DashboardOverview(
                clinic = DashboardClinic(
                    id = clinic.data.id,
                    name = clinic.data.name,
                    area = clinic.data.area,
                    city = clinic.data.city,
                ),
                // The public doctor search only returns active doctors, so the roster size is the active count.
                totalDoctorCount = roster.size,
                activeDoctorCount = roster.size,
                activeDoctors = roster.take(ROSTER_PREVIEW).map {
                    RosterDoctor(id = it.id, name = it.name, specializationName = it.specializationName.ifBlank { null })
                },
                queueWaiting = queues.sumOf { (it as ApiResult.Success).data.waiting.size },
                // Per-patient wait estimates live in the database, not in this route; don't invent an average.
                avgWaitMinutes = null,
            ),
        )
    }

    /**
     * The staff member's own clinic's appointments for [scope] (staff branch of `GET /api/appointments?scope=`).
     * The clinic is resolved on the server from the signed-in staff session; none is sent. [AppointmentPage.total]
     * is the server's count, which can exceed the list when the server capped it.
     */
    suspend fun appointments(scope: AppointmentScope): ApiResult<AppointmentPage> =
        when (val result = safeApiCall(gson) { api.listAppointments(scope.apiValue) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                AppointmentPage(
                    items = result.data,
                    total = result.meta?.get("total")?.takeIf { it.isJsonPrimitive }?.asInt ?: result.data.size,
                ),
            )
        }

    suspend fun analytics(): ApiResult<AnalyticsData> = safeApiCall(gson) { api.getDashboardAnalytics() }

    suspend fun members(): ApiResult<List<StaffMember>> = safeApiCall(gson) { api.getStaffMembers() }

    suspend fun invite(name: String, email: String, role: String): ApiResult<StaffMember> =
        safeApiCall(gson) { api.inviteStaff(InviteStaffRequest(email = email, name = name, role = role)) }

    suspend fun clinicDoctors(clinicId: String): ApiResult<List<Doctor>> =
        safeApiCall(gson) { api.searchDoctors(clinicId = clinicId, sortBy = "avgRating", pageSize = 50) }

    suspend fun createDoctor(request: CreateDoctorRequest): ApiResult<Doctor> =
        safeApiCall(gson) { api.createDoctor(request) }

    suspend fun deactivateDoctor(id: String): ApiResult<Unit> =
        safeApiCall(gson) { api.deactivateDoctor(id) }.map { }

    private companion object {
        const val ROSTER_PREVIEW = 6
    }
}
