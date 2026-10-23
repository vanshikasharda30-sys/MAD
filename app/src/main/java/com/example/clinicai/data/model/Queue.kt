package com.example.clinicai.data.model

data class QueueEntry(
    val id: String,
    val appointmentId: String? = null,
    val doctorId: String,
    val queueNumber: Int,
    val position: Int = 0,
    val status: String = "waiting",
)

data class CurrentlyServing(
    val queueNumber: Int,
    val patientName: String? = null,
)

data class WaitingEntry(
    val queueEntryId: String,
    val queueNumber: Int,
    val position: Int,
    val estimatedWaitMinutes: Int,
)

data class QueueStatus(
    val doctorId: String,
    val currentlyServing: CurrentlyServing? = null,
    val waiting: List<WaitingEntry> = emptyList(),
)

data class QueueActionResult(val done: Boolean = false)
