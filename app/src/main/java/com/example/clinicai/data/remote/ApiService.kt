package com.example.clinicai.data.remote

import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.AnalyticsData
import com.example.clinicai.data.model.AvailabilityData
import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.model.CancelResult
import com.example.clinicai.data.model.ChatReply
import com.example.clinicai.data.model.ChatRequest
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.CreateDoctorRequest
import com.example.clinicai.data.model.DashboardOverview
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.Faq
import com.example.clinicai.data.model.InviteStaffRequest
import com.example.clinicai.data.model.JoinQueueRequest
import com.example.clinicai.data.model.PaymentOrder
import com.example.clinicai.data.model.QueueActionResult
import com.example.clinicai.data.model.QueueEntry
import com.example.clinicai.data.model.QueueStatus
import com.example.clinicai.data.model.Review
import com.example.clinicai.data.model.ReviewRequest
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.data.model.StaffMember
import com.example.clinicai.data.model.UpdateClinicRequest
import com.example.clinicai.data.model.UserProfile
import com.example.clinicai.data.model.VerifyPaymentRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Typed view of the ClinicAI Next.js API routes (the `src/app/api` route handlers). */
interface ApiService {

    // ---- Public -------------------------------------------------------------------------------
    @GET("api/specializations")
    suspend fun getSpecializations(): Response<ApiEnvelope<List<Specialization>>>

    @GET("api/doctors/search")
    suspend fun searchDoctors(
        @Query("specializationId") specializationId: String? = null,
        @Query("clinicId") clinicId: String? = null,
        @Query("city") city: String? = null,
        @Query("area") area: String? = null,
        @Query("sortBy") sortBy: String = "avgRating",
        @Query("order") order: String = "desc",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): Response<ApiEnvelope<List<Doctor>>>

    @GET("api/doctors/nearby")
    suspend fun nearbyDoctors(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Int = 15,
        @Query("specializationId") specializationId: String? = null,
    ): Response<ApiEnvelope<List<Doctor>>>

    /** Added to the backend for the mobile app (`GET /api/doctors/{id}`). */
    @GET("api/doctors/{id}")
    suspend fun getDoctor(@Path("id") id: String): Response<ApiEnvelope<Doctor>>

    @GET("api/doctors/{id}/availability")
    suspend fun getAvailability(
        @Path("id") doctorId: String,
        @Query("date") date: String,
    ): Response<ApiEnvelope<AvailabilityData>>

    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ApiEnvelope<ChatReply>>

    @GET("api/clinics/{id}/faqs")
    suspend fun getClinicFaqs(@Path("id") clinicId: String): Response<ApiEnvelope<List<Faq>>>

    @GET("api/clinics/qr/{token}")
    suspend fun resolveClinicQr(@Path("token") token: String): Response<ApiEnvelope<Clinic>>

    @GET("api/queue/{doctorId}/status")
    suspend fun getQueueStatus(@Path("doctorId") doctorId: String): Response<ApiEnvelope<QueueStatus>>

    // ---- Patient (Clerk session required) ----------------------------------------------------
    @GET("api/me")
    suspend fun getMe(): Response<ApiEnvelope<UserProfile>>

    @POST("api/appointments")
    suspend fun bookAppointment(@Body request: BookAppointmentRequest): Response<ApiEnvelope<Appointment>>

    /** Added to the backend for the mobile app. */
    @GET("api/appointments")
    suspend fun listAppointments(@Query("scope") scope: String? = null): Response<ApiEnvelope<List<Appointment>>>

    /** Added to the backend for the mobile app. */
    @GET("api/appointments/{id}")
    suspend fun getAppointment(@Path("id") id: String): Response<ApiEnvelope<Appointment>>

    @PATCH("api/appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: String): Response<ApiEnvelope<CancelResult>>

    @POST("api/appointments/{id}/payment/create-order")
    suspend fun createPaymentOrder(@Path("id") id: String): Response<ApiEnvelope<PaymentOrder>>

    @POST("api/appointments/{id}/payment/verify")
    suspend fun verifyPayment(
        @Path("id") id: String,
        @Body request: VerifyPaymentRequest,
    ): Response<ApiEnvelope<Appointment>>

    @POST("api/reviews")
    suspend fun createReview(@Body request: ReviewRequest): Response<ApiEnvelope<Review>>

    @POST("api/queue/join")
    suspend fun joinQueue(@Body request: JoinQueueRequest): Response<ApiEnvelope<QueueEntry>>

    // ---- Staff ------------------------------------------------------------------------------
    @GET("api/dashboard/overview")
    suspend fun getDashboardOverview(): Response<ApiEnvelope<DashboardOverview>>

    @GET("api/dashboard/analytics")
    suspend fun getDashboardAnalytics(): Response<ApiEnvelope<AnalyticsData>>

    @GET("api/staff")
    suspend fun getStaffMembers(): Response<ApiEnvelope<List<StaffMember>>>

    @POST("api/staff/invite")
    suspend fun inviteStaff(@Body request: InviteStaffRequest): Response<ApiEnvelope<StaffMember>>

    @POST("api/doctors")
    suspend fun createDoctor(@Body request: CreateDoctorRequest): Response<ApiEnvelope<Doctor>>

    @POST("api/doctors/{id}/deactivate")
    suspend fun deactivateDoctor(@Path("id") id: String): Response<ApiEnvelope<Map<String, Boolean>>>

    @GET("api/clinics/me")
    suspend fun getMyClinic(): Response<ApiEnvelope<Clinic>>

    @PATCH("api/clinics/me")
    suspend fun updateMyClinic(@Body request: UpdateClinicRequest): Response<ApiEnvelope<Clinic>>

    @POST("api/queue/{entryId}/{action}")
    suspend fun queueAction(
        @Path("entryId") entryId: String,
        @Path("action") action: String,
    ): Response<ApiEnvelope<QueueActionResult>>
}
