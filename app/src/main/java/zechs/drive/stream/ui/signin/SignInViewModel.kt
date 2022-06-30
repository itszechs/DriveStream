package zechs.drive.stream.ui.signin

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
        authCode: String
    ) = viewModelScope.launch {
        _loginStatus.postValue(Resource.Loading())
        val response = driveRepository.get().fetchRefreshToken(authCode)
        _loginStatus.postValue(response)
    }

}
