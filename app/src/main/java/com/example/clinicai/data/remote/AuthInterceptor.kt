package com.example.clinicai.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <Clerk session JWT>` when the user is signed in. The backend's Clerk
 * middleware accepts the session token from this header exactly as it does from the browser cookie.
 * Tokens are never logged.
 */
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder().header("Accept", "application/json")
        // OkHttp interceptors run on a network thread, so blocking here is safe.
        val token = runBlocking { runCatching { tokenProvider.currentToken() }.getOrNull() }
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
