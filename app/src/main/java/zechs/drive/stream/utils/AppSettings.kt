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
        const val VIDEO_PLAYER = "VIDEO_PLAYER"
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