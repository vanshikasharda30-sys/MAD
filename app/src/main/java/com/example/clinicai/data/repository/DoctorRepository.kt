package com.example.clinicai.data.repository

import com.example.clinicai.config.AppConstants
import com.example.clinicai.data.model.AvailabilityData
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.remote.map
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson

class DoctorRepository(private val api: ApiService, private val gson: Gson) {

    suspend fun specializations(): ApiResult<List<Specialization>> =
        safeApiCall(gson) { api.getSpecializations() }

    suspend fun search(
        specializationId: String? = null,
        clinicId: String? = null,
        pageSize: Int = AppConstants.DOCTOR_PAGE_SIZE,
    ): ApiResult<List<Doctor>> = safeApiCall(gson) {
        api.searchDoctors(specializationId = specializationId, clinicId = clinicId, pageSize = pageSize)
    }

    suspend fun nearby(lat: Double, lng: Double, radiusKm: Int = 15): ApiResult<List<Doctor>> =
        safeApiCall(gson) { api.nearbyDoctors(lat, lng, radiusKm) }

    /**
     * `GET /api/doctors/{id}` was added to the backend for the app. Until that deployment is live the
     * route answers 404/405, so fall back to the public search (same data) and pick the doctor out.
     */
    suspend fun doctor(id: String): ApiResult<Doctor> {
        val direct = safeApiCall(gson) { api.getDoctor(id) }
        if (!(direct is ApiResult.Failure && direct.error is AppError.EndpointMissing)) return direct
        return when (val all = allDoctors()) {
            is ApiResult.Success ->
                all.data.firstOrNull { it.id == id }?.let { ApiResult.Success(it) }
                    ?: ApiResult.Failure(AppError.NotFound("Doctor not found"))
            is ApiResult.Failure -> all
        }
    }

    /** Every active doctor, following pagination (the API caps a page at 50). */
    suspend fun allDoctors(): ApiResult<List<Doctor>> {
        val doctors = mutableListOf<Doctor>()
        var page = 1
        while (true) {
            val currentPage = page
            val result = safeApiCall(gson) { api.searchDoctors(page = currentPage, pageSize = AppConstants.DOCTOR_PAGE_SIZE) }
            when (result) {
                is ApiResult.Failure -> return result
                is ApiResult.Success -> {
                    doctors += result.data
                    val totalPages = result.meta?.get("totalPages")?.asInt ?: 1
                    if (currentPage >= totalPages || result.data.isEmpty()) return ApiResult.Success(doctors)
                    page++
                }
            }
        }
    }

    suspend fun availability(doctorId: String, date: String): ApiResult<AvailabilityData> =
        safeApiCall(gson) { api.getAvailability(doctorId, date) }

    /**
     * The website's footer lists partner clinics straight from the database. There is no clinics
     * endpoint, so they are derived from the doctors the public search returns.
     */
    suspend fun partnerClinics(): ApiResult<List<Clinic>> =
        allDoctors().map { doctors ->
            doctors.mapNotNull { it.clinic }
                .distinctBy { it.id }
                .sortedBy { it.name }
        }
}
