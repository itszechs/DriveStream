package zechs.drive.stream.ui.files

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.drive.Drive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.data.remote.DriveHelper
import zechs.drive.stream.ui.files.FilesFragment.Companion.TAG
import zechs.drive.stream.utils.state.Resource
import java.net.SocketTimeoutException
import javax.inject.Inject


@HiltViewModel
class FilesViewModel @Inject constructor(
    private val driveHelper: DriveHelper
) : ViewModel() {

    data class LoadingState(
        val isAtLast: Boolean,
        val isLoading: Boolean
    )

    private val _userAuth = MutableLiveData<Intent?>()
    val userAuth: LiveData<Intent?>
        get() = _userAuth

    private val _filesList = MutableLiveData<Resource<List<DriveFile>>>()
    val filesList: LiveData<Resource<List<DriveFile>>>
        get() = _filesList

    private val _atLastItem = MutableLiveData<LoadingState>()
    val atLastItem: LiveData<LoadingState>
        get() = _atLastItem

    private var nextPageToken: String? = null
    private var response: MutableList<DriveFile>? = null
    private val pageSize = 25

    var hasLoaded = false

    var isLastPage = nextPageToken == null
        private set

    fun queryFiles(query: String?) = viewModelScope.launch(Dispatchers.IO) {
        _filesList.postValue(Resource.Loading())
        try {
            val drive = driveHelper.drive
            if (drive != null) {
                /**
                 * Ensures that current scope
                 * else throw CancellationException
                 */
                ensureActive()

                if (query != null) {
                    getQuery(drive, query)
                } else {
                    getTeamDrives(drive)
                }
            } else {
                _filesList.postValue(Resource.Error("Unable to build Drive Service"))
                Log.d(TAG, "Unable to build Drive Service")
            }
        } catch (user: UserRecoverableAuthIOException) {
            /*
             * https://cloud.google.com/java/docs/reference/google-api-client/latest/com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
             * This is a thing apparently
             */

            Log.d(TAG, user.message ?: "UserRecoverableAuthIOException")
            _userAuth.postValue(user.intent)
        } catch (cancel: CancellationException) {
            Log.d(TAG, cancel.message ?: "CancellationException")
        } catch (timeout: SocketTimeoutException) {
            _filesList.postValue(Resource.Error("Server timed out"))
            Log.d(TAG, timeout.message ?: "SocketTimeoutException")
        } catch (e: Exception) {
            _filesList.postValue(Resource.Error(e.message ?: "Something went wrong"))
            Log.e(TAG, "Something went wrong", e)
        }
    }

    private suspend fun getQuery(drive: Drive, query: String) {
        return suspendCancellableCoroutine {
            val files = drive
                .files()
                .list()
                .setQ(query)
                .setFields("nextPageToken, files(id, name, size, mimeType, iconLink)")
                .setPageSize(pageSize)
                .setOrderBy("folder, name")
                .setPageToken(nextPageToken)
                .setIncludeTeamDriveItems(true)
                .setSupportsTeamDrives(true)
                .execute()

            Log.d(TAG, files.toString())

            nextPageToken = files.nextPageToken
            isLastPage = nextPageToken == null

            try {
                val filesList = files.files.map {
                    DriveFile(
                        id = it.id,
                        name = it.name,
                        size = it.getSize(),
                        mimeType = it.mimeType,
                        iconLink = it.iconLink
                    )
                }.distinctBy { it.id }.toMutableList()

                response = if (response == null) {
                    filesList
                } else {
                    response!!.addAll(filesList)
                    response
                }
                _filesList.postValue(Resource.Success(response!!))
            } catch (npe: NullPointerException) {
                _filesList.postValue(Resource.Error(files.toString()))
            }
        }
    }

    private suspend fun getTeamDrives(drive: Drive) {
        return suspendCancellableCoroutine {
            val teamDrives = drive
                .teamdrives()
                .list()
                .execute()

            Log.d(TAG, teamDrives.toString())

            val sharedDrives = teamDrives.teamDrives.map {
                DriveFile(
                    id = it.id,
                    name = it.name,
                    size = null,
                    mimeType = it.kind,
                    iconLink = null
                )
            }.distinctBy { it.id }.sortedBy { it.name }

            _filesList.postValue(Resource.Success(sharedDrives))
        }
    }

    fun setLoadingState(atLastItem: Boolean, isLoading: Boolean) {
        if (_atLastItem.value == LoadingState(atLastItem, isLoading)) return
        _atLastItem.value = LoadingState(atLastItem, isLoading)
    }


    fun handleSignInResult(result: Intent) = viewModelScope.launch {
        driveHelper.handleSignIn(result)
    }

}
