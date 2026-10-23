package com.example.clinicai

import android.content.Context
import com.example.clinicai.config.ApiConfig
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.auth.PasswordPolicyRepository
import com.example.clinicai.data.local.ReceiptStore
import com.example.clinicai.data.location.LocationRepository
import com.example.clinicai.data.local.SessionPreferences
import com.example.clinicai.data.remote.ApiClient
import com.example.clinicai.data.remote.PaymentResultBus
import com.example.clinicai.data.repository.AppointmentRepository
import com.example.clinicai.data.repository.ChatRepository
import com.example.clinicai.data.repository.ClinicRepository
import com.example.clinicai.data.repository.DoctorRepository
import com.example.clinicai.data.repository.PaymentRepository
import com.example.clinicai.data.repository.QueueRepository
import com.example.clinicai.data.repository.ReceiptRepository
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.data.repository.UserRepository

/** Manual dependency container: one instance for the process, created in [ClinicAIApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val gson = ApiClient.gson
    val auth = ClerkAuthManager(BuildConfig.CLERK_PUBLISHABLE_KEY)
    val passwordPolicy = PasswordPolicyRepository(BuildConfig.CLERK_PUBLISHABLE_KEY)
    val prefs = SessionPreferences(appContext)
    val paymentResults = PaymentResultBus()
    val location = LocationRepository.create(appContext)

    private val api = ApiClient.service(ApiConfig.baseUrl, ApiClient.okHttp(auth), gson)

    val doctors = DoctorRepository(api, gson)
    val appointments = AppointmentRepository(api, gson)
    val payments = PaymentRepository(api, gson)
    val chat = ChatRepository(api, gson, doctors)
    val clinics = ClinicRepository(api, gson)
    val queue = QueueRepository(api, gson)
    val users = UserRepository(api, gson, prefs)
    val staff = StaffRepository(api, gson)
    val receipts = ReceiptRepository(ReceiptStore(appContext, gson), auth, users)
}
