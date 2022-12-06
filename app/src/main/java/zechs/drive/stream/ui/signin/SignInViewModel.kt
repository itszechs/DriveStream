package zechs.drive.stream.ui.signin

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import zechs.drive.stream.data.model.AuthorizationResponse
import zechs.drive.stream.data.repository.DriveRepository
import zechs.drive.stream.utils.state.Resource
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val driveRepository: Lazy<DriveRepository>
) : ViewModel() {

    private val _loginStatus = MutableLiveData<Resource<AuthorizationResponse>>()
    val loginStatus: LiveData<Resource<AuthorizationResponse>>
        get() = _loginStatus

    fun requestRefreshToken(
        authCodeUri: String
    ) = viewModelScope.launch {
        _loginStatus.postValue(Resource.Loading())
        val authCode = Uri.parse(authCodeUri).getQueryParameter("code")
        if (authCode == null) {
            _loginStatus.postValue(Resource.Error("Authorization code not found, please check url"))
        } else {
            val response = driveRepository.get().fetchRefreshToken(authCode)
            _loginStatus.postValue(response)
        }
    }

}
