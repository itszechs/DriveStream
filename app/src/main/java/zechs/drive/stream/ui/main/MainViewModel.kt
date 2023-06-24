package zechs.drive.stream.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.data.model.LatestRelease
import zechs.drive.stream.data.repository.GithubRepository
import zechs.drive.stream.utils.AppSettings
import zechs.drive.stream.utils.AppTheme
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.VideoPlayer
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val githubRepository: GithubRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasLoggedIn = MutableStateFlow(false)
    val hasLoggedIn = _hasLoggedIn.asStateFlow()

    private val _latest = MutableLiveData<Resource<LatestRelease>>()
    val latest: LiveData<Resource<LatestRelease>>
        get() = _latest

    private val _theme = MutableSharedFlow<AppTheme>()
    val theme = _theme.asSharedFlow()

    var currentThemeIndex = 2
        private set

    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated = _lastUpdated.asStateFlow()

    var isChecking = false
        private set

    private val _showAds = MutableLiveData<Boolean>()
    val showAds: LiveData<Boolean> = _showAds

    var adsEnabled = false
        private set

    init {
        viewModelScope.launch {
            getTheme()
            getEnableAds()
            val status = getLoginStatus()
            if (status) {
                getPlayer()
                getLastUpdated()
            }
            _hasLoggedIn.value = status
            delay(250L)
            _isLoading.value = false
        }
        getLatestRelease() // check for update
    }

    private suspend fun getLoginStatus(): Boolean {
        sessionManager.fetchClient() ?: return false
        sessionManager.fetchRefreshToken() ?: return false
        return true
    }

    fun getLatestRelease() = viewModelScope.launch {
        _latest.postValue(Resource.Loading())
        isChecking = true
        _latest.postValue(githubRepository.getLatestRelease())
        isChecking = false
        appSettings.saveLastUpdated()
        getLastUpdated()
    }

    private fun getLastUpdated() = viewModelScope.launch {
        _lastUpdated.emit(appSettings.fetchLastUpdated())
    }

    private suspend fun getTheme() {
        val fetchTheme = appSettings.fetchTheme()
        currentThemeIndex = fetchTheme.value
        _theme.emit(fetchTheme)
    }

    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        appSettings.saveTheme(theme)
        getTheme()
    }

    var currentPlayerIndex = VideoPlayer.MPV
        private set

    private suspend fun getPlayer() {
        val fetchPlayer = appSettings.fetchPlayer()
        currentPlayerIndex = fetchPlayer
    }

    fun setPlayer(player: VideoPlayer) = viewModelScope.launch {
        appSettings.savePlayer(player)
        getPlayer()
    }

    private suspend fun getEnableAds() {
        val fetchEnableAds = appSettings.fetchEnableAds()
        adsEnabled = fetchEnableAds
        _showAds.postValue(fetchEnableAds)
    }

    fun setEnableAds(enable: Boolean) = viewModelScope.launch {
        appSettings.saveEnableAds(enable)
        getEnableAds()
    }

}
