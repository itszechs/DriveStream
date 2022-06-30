package zechs.drive.stream.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.utils.SessionManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasLoggedIn = MutableStateFlow(false)
    val hasLoggedIn = _hasLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            val status = getLoginStatus()
            delay(250L)
            _isLoading.value = false
            _hasLoggedIn.value = status
        }
    }

    private suspend fun getLoginStatus(): Boolean {
        val refreshToken = sessionManager.fetchRefreshToken()
        return refreshToken != null
    }

}
