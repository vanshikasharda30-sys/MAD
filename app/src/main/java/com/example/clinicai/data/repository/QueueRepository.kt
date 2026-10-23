package com.example.clinicai.data.repository

import com.example.clinicai.data.model.JoinQueueRequest
import com.example.clinicai.data.model.QueueActionResult
import com.example.clinicai.data.model.QueueEntry
import com.example.clinicai.data.model.QueueStatus
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.ApiService
import com.example.clinicai.data.remote.safeApiCall
import com.google.gson.Gson

class QueueRepository(private val api: ApiService, private val gson: Gson) {

    suspend fun join(doctorId: String): ApiResult<QueueEntry> =
        safeApiCall(gson) { api.joinQueue(JoinQueueRequest(doctorId)) }

    suspend fun status(doctorId: String): ApiResult<QueueStatus> =
        safeApiCall(gson) { api.getQueueStatus(doctorId) }

    /** Staff only. [action] is `call`, `skip` or `complete`. */
    suspend fun perform(entryId: String, action: QueueAction): ApiResult<QueueActionResult> =
        safeApiCall(gson) { api.queueAction(entryId, action.path) }

    enum class QueueAction(val path: String) { CALL("call"), SKIP("skip"), COMPLETE("complete") }
}
