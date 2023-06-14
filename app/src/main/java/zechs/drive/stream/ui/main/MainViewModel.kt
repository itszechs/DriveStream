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
import zechs.drive.stream.utils.AppTheme
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.ThemeManager
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val githubRepository: GithubRepository,
    private val themeManager: ThemeManager
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

    init {
        viewModelScope.launch {
            getTheme()
            val status = getLoginStatus()
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

    private fun getLatestRelease() = viewModelScope.launch {
        _latest.postValue(githubRepository.getLatestRelease())
    }

    private suspend fun getTheme() {
        val fetchTheme = themeManager.fetchTheme()
        currentThemeIndex = fetchTheme.value
        _theme.emit(fetchTheme)
    }

    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        themeManager.saveTheme(theme)
        getTheme()
    }

}
