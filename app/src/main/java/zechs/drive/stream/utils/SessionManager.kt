package zechs.drive.stream.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext appContext: Context
) {

    private val sessionStore = appContext.dataStore

    suspend fun saveAccessToken(accessToken: String) {
        val dataStoreKey = stringPreferencesKey(ACCESS_TOKEN)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = accessToken
        }
        Log.d(TAG, "saveAccessToken: $accessToken")
    }

    suspend fun fetchAccessToken(): String? {
        val dataStoreKey = stringPreferencesKey(ACCESS_TOKEN)
        val preferences = sessionStore.data.first()
        val value = preferences[dataStoreKey]
        Log.d(TAG, "fetchAccessToken: $value")
        return value
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(
            "DRIVE_SESSION"
        )
        const val TAG = "SessionManager"
        const val ACCESS_TOKEN = "ACCESS_TOKEN"
    }

}