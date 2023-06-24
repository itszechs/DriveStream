package zechs.drive.stream.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import zechs.drive.stream.utils.util.Converter
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
        const val VIDEO_PLAYER = "VIDEO_PLAYER"
        const val LAST_UPDATED = "LAST_UPDATED"
        const val ENABLE_ADS = "ENABLE_ADS"
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

    suspend fun savePlayer(player: VideoPlayer) {
        val dataStoreKey = stringPreferencesKey(VIDEO_PLAYER)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = player.text
        }
        Log.d(TAG, "savePlayer: ${player.text}")
    }

    suspend fun fetchPlayer(): VideoPlayer {
        val dataStoreKey = stringPreferencesKey(VIDEO_PLAYER)
        val preferences = sessionStore.data.first()
        val videoPlayer = when (preferences[dataStoreKey]) {
            VideoPlayer.EXO_PLAYER.text -> VideoPlayer.EXO_PLAYER
            else -> VideoPlayer.MPV
        }
        Log.d(TAG, "fetchPlayer: $videoPlayer")
        return videoPlayer
    }

    suspend fun saveLastUpdated() {
        val dataStoreKey = stringPreferencesKey(LAST_UPDATED)
        val lastUpdated = System.currentTimeMillis()
        sessionStore.edit { settings ->
            settings[dataStoreKey] = lastUpdated.toString()
        }
        Log.d(TAG, "saveLastUpdated: ${Converter.fromTimeInMills(lastUpdated)}")
    }

    suspend fun fetchLastUpdated(): String? {
        val dataStoreKey = stringPreferencesKey(LAST_UPDATED)
        val preferences = sessionStore.data.first()
        val lastUpdated = preferences[dataStoreKey] ?: return null
        val parsed = Converter.fromTimeInMills(lastUpdated.toLong())
        Log.d(TAG, "fetchLastUpdated: $parsed")
        return parsed
    }

    suspend fun saveEnableAds(enable: Boolean) {
        val dataStoreKey = stringPreferencesKey(ENABLE_ADS)
        sessionStore.edit { settings ->
            settings[dataStoreKey] = enable.toString()
        }
        Log.d(TAG, "saveEnableAds: $enable")
    }

    suspend fun fetchEnableAds(): Boolean {
        val dataStoreKey = stringPreferencesKey(ENABLE_ADS)
        val preferences = sessionStore.data.first()
        val enable = preferences[dataStoreKey] ?: return true
        Log.d(TAG, "fetchEnableAds: $enable")
        return enable.toBoolean()
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

enum class VideoPlayer(
    val text: String,
    val value: Int
) {
    EXO_PLAYER("ExoPlayer", 0),
    MPV("MPV", 1),

}