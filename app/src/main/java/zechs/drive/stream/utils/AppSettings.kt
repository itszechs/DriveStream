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
class AppSettings @Inject constructor(
    @ApplicationContext appContext: Context
) {

    companion object {
        private val Context.dataStore by preferencesDataStore(
            "APP_SETTINGS"
        )
        const val TAG = "AppSettings"
        const val APP_THEME = "APP_THEME"
    }

    private val sessionStore = appContext.dataStore

    suspend fun saveTheme(theme: AppTheme) {
        val dataStoreKey = stringPreferencesKey(APP_THEME)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = theme.text
        }
        Log.d(TAG, "saveTheme: ${theme.text}")
    }

    suspend fun fetchTheme(): AppTheme {
        val dataStoreKey = stringPreferencesKey(APP_THEME)
        val preferences = sessionStore.data.first()
        val appTheme = when (preferences[dataStoreKey]) {
            AppTheme.LIGHT.text -> AppTheme.LIGHT
            AppTheme.DARK.text -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
        Log.d(TAG, "fetchTheme: $appTheme")
        return appTheme
    }

}

enum class AppTheme(
    val text: String,
    val value: Int
) {
    DARK("Dark", 0),
    LIGHT("Light", 1),
    SYSTEM("System", 2)
}