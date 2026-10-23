package com.example.clinicai.data.model

data class Faq(
    val id: String,
    val clinicId: String = "",
    val question: String,
    val answer: String,
)
