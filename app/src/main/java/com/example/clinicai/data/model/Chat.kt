package com.example.clinicai.data.model

/** `POST /api/chat` -> `data`. `type` is one of: emergency, symptom_match, faq_answer, booking_prompt, greeting, smalltalk, fallback. */
data class ChatReply(
    val type: String,
    val reply: String,
    val specializationId: String? = null,
    val specializationName: String? = null,
)

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val type: String? = null,
    val specializationId: String? = null,
    val suggestedDoctors: List<Doctor> = emptyList(),
    val isError: Boolean = false,
) {
    val isEmergency: Boolean get() = type == "emergency"
}
