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
import zechs.drive.stream.ui.files.adapter.FilesDataModel
import zechs.drive.stream.utils.state.Resource
import java.net.SocketTimeoutException
import javax.inject.Inject


@HiltViewModel
class FilesViewModel @Inject constructor(
    private val driveHelper: DriveHelper
) : ViewModel() {

    private val _userAuth = MutableLiveData<Intent?>()
    val userAuth: LiveData<Intent?>
        get() = _userAuth

    private val _filesList = MutableLiveData<Resource<List<FilesDataModel>>>()
    val filesList: LiveData<Resource<List<FilesDataModel>>>
        get() = _filesList

    private var nextPageToken: String? = null
    private var response: MutableList<FilesDataModel>? = null
    private val pageSize = 25

    var hasLoaded = false

    var hasFailed = false
        private set

    var isLastPage = nextPageToken == null
        private set

    fun queryFiles(query: String?) = viewModelScope.launch(Dispatchers.IO) {
        _filesList.postValue(Resource.Loading())
        try {
            val drive = driveHelper.drive
            if (drive != null) {
                /**
                 * Ensures that current scope is active
                 * else throw CancellationException
                 */
                ensureActive()

                if (query != null) {
                    getQuery(drive, query)
                } else {
                    getTeamDrives(drive)
                }
                hasFailed = false
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
        hasFailed = true
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

            val filesDataModel = mutableListOf<FilesDataModel>()

            try {
                val filesList = files.files
                    .map {
                        FilesDataModel.File(
                            DriveFile(
                                id = it.id,
                                name = it.name,
                                size = it.getSize(),
                                mimeType = it.mimeType,
                                iconLink = it.iconLink
                            )
                        )
                    }.distinctBy { it.driveFile.id }

                response = if (response == null) {
                    filesDataModel.addAll(filesList)
                    filesDataModel
                } else {
                    // append new list of files
                    response!!.addAll(filesList)

                    // return new list and remove all Loading
                    response!!.filter {
                        it != FilesDataModel.Loading
                    }.toMutableList()
                }

                // before submitting add Loading
                // if list is not at last page
                if (!isLastPage) {
                    response!!.add(FilesDataModel.Loading)
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
                .setFields("nextPageToken, teamDrives(id, name, kind)")
                .setPageSize(pageSize)
                .setPageToken(nextPageToken)
                .execute()

            Log.d(TAG, teamDrives.toString())
            nextPageToken = teamDrives.nextPageToken
            isLastPage = nextPageToken == null

            val filesDataModel = mutableListOf<FilesDataModel>()
            val sharedDrives = teamDrives.teamDrives
                .map {
                    FilesDataModel.File(
                        DriveFile(
                            id = it.id,
                            name = it.name,
                            size = null,
                            mimeType = it.kind,
                            iconLink = null
                        )
                    )
                }
                .distinctBy { it.driveFile.id }
                .sortedBy { it.driveFile.name }

            response = if (response == null) {
                filesDataModel.addAll(sharedDrives)
                filesDataModel
            } else {
                // append new list of files
                response!!.addAll(sharedDrives)

                // return new list and remove all Loading
                response!!.filter {
                    it != FilesDataModel.Loading
                }.toMutableList()
            }

            // before submitting add Loading
            // if list is not at last page
            if (!isLastPage) {
                response!!.add(FilesDataModel.Loading)
            }

            _filesList.postValue(Resource.Success(filesDataModel.toList()))
        }
    }

    fun handleSignInResult(result: Intent) = viewModelScope.launch {
        driveHelper.handleSignIn(result)
    }

}
