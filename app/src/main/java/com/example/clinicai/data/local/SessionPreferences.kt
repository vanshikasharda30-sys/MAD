package com.example.clinicai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.clinicai.domain.SymptomNotesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "clinicai_session")

/**
 * Non-sensitive app state kept across launches (DataStore, app-private, excluded from backups).
 * Credentials and tokens are NOT stored here — Clerk's SDK owns the session.
 */
class SessionPreferences(private val context: Context) : SymptomNotesStore {

    private object Keys {
        val SYMPTOM_NOTES = stringPreferencesKey("symptom_notes")
        val LAST_ROLE = stringPreferencesKey("last_role")
        val PENDING_DOCTOR_ID = stringPreferencesKey("pending_doctor_id")
        val PENDING_DATE = stringPreferencesKey("pending_date")
        val PENDING_SLOT = stringPreferencesKey("pending_slot")
    }

    /** Symptom text from a chat triage; prefilled into the next booking, like the website's localStorage note. */
    val symptomNotes: Flow<String?> = context.sessionDataStore.data.map { it[Keys.SYMPTOM_NOTES] }

    suspend fun saveSymptomNotes(notes: String) {
        context.sessionDataStore.edit { it[Keys.SYMPTOM_NOTES] = notes.take(500) }
    }

    override suspend fun clearSymptomNotes() {
        context.sessionDataStore.edit { it.remove(Keys.SYMPTOM_NOTES) }
    }

    override suspend fun readSymptomNotes(): String? = symptomNotes.first()

    /** Last role seen from `/api/me`, used only to route offline launches. */
    suspend fun lastRole(): String? = context.sessionDataStore.data.first()[Keys.LAST_ROLE]

    suspend fun saveLastRole(role: String?) {
        context.sessionDataStore.edit { prefs ->
            if (role == null) prefs.remove(Keys.LAST_ROLE) else prefs[Keys.LAST_ROLE] = role
        }
    }

    suspend fun clearUserState() {
        context.sessionDataStore.edit {
            it.remove(Keys.SYMPTOM_NOTES)
            it.remove(Keys.LAST_ROLE)
        }
    }
}
