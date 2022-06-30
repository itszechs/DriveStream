package zechs.drive.stream.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.utils.SessionManager
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: Lazy<SessionManager>
) : ViewModel() {

    private val _hasLoggedOut = MutableStateFlow(false)
    val hasLoggedOut = _hasLoggedOut.asStateFlow()

    fun logOut() = viewModelScope.launch {
        delay(250L)
        sessionManager.get().resetDataStore()
        _hasLoggedOut.value = true
    }

}
