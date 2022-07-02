package zechs.drive.stream.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.data.model.LatestRelease
import zechs.drive.stream.data.repository.GithubRepository
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val githubRepository: GithubRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasLoggedIn = MutableStateFlow(false)
    val hasLoggedIn = _hasLoggedIn.asStateFlow()

    private val _latest = MutableLiveData<Resource<LatestRelease>>()
    val latest: LiveData<Resource<LatestRelease>>
        get() = _latest

    init {
        viewModelScope.launch {
            val status = getLoginStatus()
            delay(250L)
            _isLoading.value = false
            _hasLoggedIn.value = status
        }
        getLatestRelease() // check for update
    }

    private suspend fun getLoginStatus(): Boolean {
        val refreshToken = sessionManager.fetchRefreshToken()
        return refreshToken != null
    }

    private fun getLatestRelease() = viewModelScope.launch {
        _latest.postValue(githubRepository.getLatestRelease())
    }
}
