package com.example.clinicai.data.local

import android.content.Context
import com.example.clinicai.domain.ReceiptData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps a copy of receipts for appointments the backend confirmed on this device, one private file
 * per signed-in user. It is a cache of server-confirmed bookings — never a place where bookings
 * are created — so a receipt can still be shown offline or before the history API is deployed.
 */
class ReceiptStore(context: Context, private val gson: Gson) {

    private val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    private val lock = Mutex()
    private val type = object : TypeToken<List<ReceiptData>>() {}.type

    suspend fun save(userId: String, receipt: ReceiptData) = lock.withLock {
        withContext(Dispatchers.IO) {
            val existing = readLocked(userId).filterNot { it.appointmentId == receipt.appointmentId }
            fileFor(userId).writeText(gson.toJson(listOf(receipt) + existing))
        }
    }

    suspend fun all(userId: String): List<ReceiptData> = lock.withLock {
        withContext(Dispatchers.IO) { readLocked(userId) }
    }

    suspend fun find(userId: String, appointmentId: String): ReceiptData? =
        all(userId).firstOrNull { it.appointmentId == appointmentId }

    private fun readLocked(userId: String): List<ReceiptData> {
        val file = fileFor(userId)
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson<List<ReceiptData>>(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fileFor(userId: String) = File(dir, userId.filter { it.isLetterOrDigit() || it == '_' } + ".json")
}
