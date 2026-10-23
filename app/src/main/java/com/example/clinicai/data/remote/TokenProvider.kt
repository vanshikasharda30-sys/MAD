package com.example.clinicai.data.remote

/** Supplies the current Clerk session JWT, or null when signed out. */
fun interface TokenProvider {
    suspend fun currentToken(): String?
}
