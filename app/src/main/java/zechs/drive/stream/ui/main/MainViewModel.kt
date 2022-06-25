package zechs.drive.stream.ui.main

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.data.remote.DriveHelper
import zechs.drive.stream.ui.main.MainActivity.Companion.TAG
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val driveHelper: DriveHelper
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasLoggedIn = MutableStateFlow(false)
    val hasLoggedIn = _hasLoggedIn.asStateFlow()

    private val _loginStatus = MutableLiveData<String>()
    val loginStatus: LiveData<String>
        get() = _loginStatus

    init {
        viewModelScope.launch {
            delay(250L)
            _isLoading.value = false
        }
    }

    fun handleSignInResult(result: Intent) = viewModelScope.launch {
        driveHelper.handleSignIn(result)
        val msg = if (driveHelper.hasSignedIn && driveHelper.drive != null) {
            _hasLoggedIn.value = true
            "Sign-in was successful"
        } else "Unable to create drive service"
        _loginStatus.value = msg
        Log.d(TAG, msg)
    }

    fun getClient() = driveHelper.getClient()
}
