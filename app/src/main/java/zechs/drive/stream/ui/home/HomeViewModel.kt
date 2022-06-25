package zechs.drive.stream.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.data.remote.DriveHelper
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val driveHelper: DriveHelper
) : ViewModel() {

    private val _hasLoggedOut = MutableStateFlow(false)
    val hasLoggedOut = _hasLoggedOut.asStateFlow()

    fun logOut() = viewModelScope.launch {
        delay(250L)
        val client = driveHelper.getClient()
        // client.revokeAccess()
        client.signOut()
        _hasLoggedOut.value = true
    }

}
