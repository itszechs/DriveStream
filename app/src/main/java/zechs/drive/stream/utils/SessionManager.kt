package zechs.drive.stream.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import zechs.drive.stream.data.local.AccountsDao
import zechs.drive.stream.data.model.DriveClient
import zechs.drive.stream.data.model.TokenResponse
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext appContext: Context,
    private val gson: Gson,
    private val accountsManager: AccountsDao,
) {

    private val sessionStore = appContext.dataStore

    suspend fun saveClient(client: DriveClient) {
        val dataStoreKey = stringPreferencesKey(DRIVE_CLIENT)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = gson.toJson(client)
        }
        Log.d(TAG, "saveClient: $client")
    }

    suspend fun fetchClient(): DriveClient? {
        val dataStoreKey = stringPreferencesKey(DRIVE_CLIENT)
        val preferences = sessionStore.data.first()
        val value = preferences[dataStoreKey]
        val client: DriveClient? = value?.let {
            val type = object : TypeToken<DriveClient?>() {}.type
            gson.fromJson(value, type)
        }
        Log.d(TAG, "fetchClient: $client")
        return client
    }

    suspend fun saveAccessToken(data: TokenResponse) {
        val dataStoreKey = stringPreferencesKey(ACCESS_TOKEN)
        val refreshToken = fetchRefreshToken()!!
        accountsManager.updateAccessToken(refreshToken, gson.toJson(data))
        sessionStore.edit { settings ->
            settings[dataStoreKey] = gson.toJson(data)
        }
        Log.d(TAG, "saveAccessToken: $data")
    }

    suspend fun fetchAccessToken(): TokenResponse? {
        val dataStoreKey = stringPreferencesKey(ACCESS_TOKEN)
        val preferences = sessionStore.data.first()
        val value = preferences[dataStoreKey]
        val login: TokenResponse? = value?.let {
            val type = object : TypeToken<TokenResponse?>() {}.type
            gson.fromJson(value, type)
        }
        Log.d(TAG, "fetchAccessToken: $login")
        return login
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        val dataStoreKey = stringPreferencesKey(REFRESH_TOKEN)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = refreshToken
        }
        Log.d(TAG, "saveRefreshToken: $refreshToken")
    }

    suspend fun fetchRefreshToken(): String? {
        val dataStoreKey = stringPreferencesKey(REFRESH_TOKEN)
        val preferences = sessionStore.data.first()
        val value = preferences[dataStoreKey]
        Log.d(TAG, "fetchRefreshToken: $value")
        return value
    }

    suspend fun saveDefault(profile: String) {
        val dataStoreKey = stringPreferencesKey(DEFAULT_PROFILE)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = profile
        }
        Log.d(TAG, "saveDefault: $profile")
    }

    suspend fun fetchDefault(): String? {
        val dataStoreKey = stringPreferencesKey(DEFAULT_PROFILE)
        val preferences = sessionStore.data.first()
        val value = preferences[dataStoreKey]
        Log.d(TAG, "fetchDefault: $value")
        return value
    }

    suspend fun resetDataStore() {
        sessionStore.edit { it.clear() }
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(
            "DRIVE_SESSION"
        )
        const val TAG = "SessionManager"
        const val DRIVE_CLIENT = "DRIVE_CLIENT"
        const val ACCESS_TOKEN = "ACCESS_TOKEN"
        const val REFRESH_TOKEN = "REFRESH_TOKEN"
        const val DEFAULT_PROFILE = "DEFAULT_PROFILE"
    }

}