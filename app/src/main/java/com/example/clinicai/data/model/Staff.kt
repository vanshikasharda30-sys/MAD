package com.example.clinicai.data.model

data class DashboardClinic(
    val id: String,
    val name: String,
    val area: String = "",
    val city: String = "",
)

data class RosterDoctor(
    val id: String,
    val name: String,
    val specializationName: String? = null,
)

data class DashboardOverview(
    val clinic: DashboardClinic? = null,
    val totalDoctorCount: Int = 0,
    val activeDoctorCount: Int = 0,
    val activeDoctors: List<RosterDoctor> = emptyList(),
    val queueWaiting: Int = 0,
    val avgWaitMinutes: Int? = null,
)

data class DailyCount(val date: String, val count: Int)

data class DoctorStat(
    val id: String,
    val name: String,
    val specializationName: String? = null,
    val appointmentCount: Int = 0,
    val noShowCount: Int = 0,
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
)

data class AnalyticsData(
    val rangeFrom: String = "",
    val rangeTo: String = "",
    val total: Int = 0,
    val completed: Int = 0,
    val noShow: Int = 0,
    val cancelled: Int = 0,
    val scheduled: Int = 0,
    val walkIn: Int = 0,
    val inProgress: Int = 0,
    val noShowRate: Double = 0.0,
    val completionRate: Double = 0.0,
    val estimatedRevenue: Int = 0,
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val dailyTrend: List<DailyCount> = emptyList(),
    val doctorStats: List<DoctorStat> = emptyList(),
)

data class StaffMember(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isActive: Boolean = true,
    val invitePending: Boolean = false,
)
