package com.example.clinicai.data.model

data class BookAppointmentRequest(
    val doctorId: String,
    val date: String,
    val startTime: String,
    val notes: String? = null,
)

data class ChatRequest(val clinicId: String, val message: String)

data class JoinQueueRequest(val doctorId: String)

data class ReviewRequest(
    val appointmentId: String,
    val rating: Int,
    val reviewText: String? = null,
)

data class CreateDoctorRequest(
    val name: String,
    val qualification: String,
    val specializationId: String,
    val experienceYears: Int,
    val consultationFee: Int,
    val slotDurationMinutes: Int,
    val avgConsultationMinutes: Int,
)

data class InviteStaffRequest(val email: String, val name: String, val role: String)

data class UpdateClinicRequest(val phone: String? = null, val email: String? = null)
