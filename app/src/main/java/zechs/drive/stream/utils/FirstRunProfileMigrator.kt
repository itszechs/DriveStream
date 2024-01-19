package zechs.drive.stream.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import zechs.drive.stream.data.local.AccountsDao
import zechs.drive.stream.data.model.Account


class FirstRunProfileMigrator(
    appContext: Context,
    private val gson: Gson,
    private val sessionManager: SessionManager,
    private val accountsManager: AccountsDao,
) {

    private val sessionStore = appContext.dataStore


    private suspend fun markFirstRun() {
        if (!isFirstRun()) return
        val dataStoreKey = stringPreferencesKey(IS_FIRST_RUN)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = true.toString()
        }
        Log.d(TAG, "First run completed")
    }

    private suspend fun isFirstRun(): Boolean {
        val dataStoreKey = stringPreferencesKey(IS_FIRST_RUN)
        val preferences = sessionStore.data.first()
        val value = preferences[dataStoreKey] ?: return true
        return value.toBoolean()
    }

    suspend fun migrateSessionToAccountsTable() {
        val client = sessionManager.fetchClient()
        if (isFirstRun() && client != null) {
            val accessToken = sessionManager.fetchAccessToken() ?: return
            val refreshToken = sessionManager.fetchRefreshToken() ?: return
            val profileName = "DriveStream"
            val account = Account(
                name = profileName,
                clientId = client.clientId,
                clientSecret = client.clientSecret,
                redirectUri = client.redirectUri,
                refreshToken = refreshToken,
                accessToken = gson.toJson(accessToken)
            )
            accountsManager.addAccount(account)
            Log.d(TAG, "migrateSessionToAccountsTable: $account")
        }
        markFirstRun()
    }

    companion object {
        private val Context.dataStore by preferencesDataStore(
            "FIRST_RUN_PROFILE_MIGRATOR"
        )
        const val TAG = "FirstRunProfileMigrator"
        const val IS_FIRST_RUN = "IS_FIRST_RUN"
    }

}