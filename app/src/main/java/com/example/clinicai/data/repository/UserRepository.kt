package com.example.clinicai.data.repository

import com.example.clinicai.data.local.SessionPreferences
import com.example.clinicai.data.model.UserProfile
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson

class UserRepository(
    private val api: ApiService,
    private val gson: Gson,
    private val prefs: SessionPreferences,
) {
    /** Who the backend thinks the signed-in Clerk user is: a PATIENT or a STAFF member. */
    suspend fun me(): ApiResult<UserProfile> =
        safeApiCall(gson) { api.getMe() }.also { result ->
            if (result is ApiResult.Success) prefs.saveLastRole(result.data.type)
        }
}
