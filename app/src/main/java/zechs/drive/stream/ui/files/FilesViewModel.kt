package zechs.drive.stream.ui.files

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.data.model.Starred
import zechs.drive.stream.data.repository.DriveRepository
import zechs.drive.stream.ui.files.FilesFragment.Companion.TAG
import zechs.drive.stream.ui.files.adapter.FilesDataModel
import zechs.drive.stream.utils.Event
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.state.Resource
import java.net.SocketTimeoutException
import javax.inject.Inject


@HiltViewModel
class FilesViewModel @Inject constructor(
    private val driveRepository: DriveRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _filesList = MutableLiveData<Resource<List<FilesDataModel>>>()
    val filesList: LiveData<Resource<List<FilesDataModel>>>
        get() = _filesList

    private val _token = MutableLiveData<Event<Resource<FileToken>>>()
    val mpvFile: LiveData<Event<Resource<FileToken>>>
        get() = _token


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

            /**
             * Ensures that current scope is active
             * else throw CancellationException
             */
            ensureActive()

            if (query != null) {
                getQuery(query)
            } else {
                getTeamDrives()
            }
            hasFailed = false
        } catch (cancel: CancellationException) {
            Log.d(TAG, cancel.message ?: "CancellationException")
        } catch (timeout: SocketTimeoutException) {
            _filesList.postValue(Resource.Error("Server timed out"))
            Log.d(TAG, timeout.message ?: "SocketTimeoutException")
            hasFailed = true
        } catch (e: Exception) {
            _filesList.postValue(Resource.Error(e.message ?: "Something went wrong"))
            Log.e(TAG, "Something went wrong", e)
            hasFailed = true
        }
    }

    private suspend fun getQuery(query: String) {
        val filesResponse = driveRepository.getFiles(
            query = query,
            pageToken = nextPageToken,
            pageSize = pageSize
        )

        when (filesResponse) {
            is Resource.Success -> {
                val files = filesResponse.data!!

                Log.d(TAG, files.toString())

                nextPageToken = files.nextPageToken
                isLastPage = nextPageToken == null

                val filesDataModel = mutableListOf<FilesDataModel>()

                val filesList = files.files
                    .map { FilesDataModel.File(it.toDriveFile()) }
                    .distinctBy { it.driveFile.id }

                postSuccess(filesDataModel, filesList)
            }
            is Resource.Error -> {
                _filesList.postValue(
                    Resource.Error(message = filesResponse.message!!)
                )
            }
            else -> {}
        }
    }

    private suspend fun getTeamDrives() {
        val drivesResponse = driveRepository.getDrives(
            pageToken = nextPageToken,
            pageSize = pageSize
        )

        when (drivesResponse) {
            is Resource.Success -> {
                val teamDrives = drivesResponse.data!!

                Log.d(TAG, teamDrives.toString())

                nextPageToken = teamDrives.nextPageToken
                isLastPage = nextPageToken == null

                val filesDataModel = mutableListOf<FilesDataModel>()
                val sharedDrives = teamDrives.drives
                    .map { FilesDataModel.File(it.toDriveFile()) }
                    .distinctBy { it.driveFile.id }

                postSuccess(filesDataModel, sharedDrives)
            }
            is Resource.Error -> {
                _filesList.postValue(Resource.Error(drivesResponse.message!!))
            }
            else -> {}
        }
    }

    private fun postSuccess(
        filesDataModel: MutableList<FilesDataModel>,
        filesList: List<FilesDataModel.File>
    ) {
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
    }

    data class FileToken(
        val fileId: String,
        val fileName: String,
        val accessToken: String
    )

    fun fetchToken(file: DriveFile) = viewModelScope.launch {
        _token.postValue(Event(Resource.Loading()))

        val client = sessionManager.fetchClient() ?: run {
            _token.postValue(Event(Resource.Error("Client not found")))
            return@launch
        }
        val tokenResponse = driveRepository.fetchAccessToken(client)

        when (tokenResponse) {
            is Resource.Success -> {
                val fileToken = FileToken(
                    fileId = file.id,
                    fileName = file.name,
                    accessToken = tokenResponse.data!!.accessToken
                )
                _token.postValue(Event(Resource.Success(fileToken)))
            }
            is Resource.Error -> {
                _token.postValue(
                    Event(Resource.Error(tokenResponse.message!!))
                )
            }
            else -> {}
        }
    }

    private val _fileUpdate = MutableSharedFlow<String>()
    val fileUpdate: SharedFlow<String>
        get() = _fileUpdate.asSharedFlow()

    fun starFile(
        file: DriveFile,
        starred: Boolean
    ) = viewModelScope.launch(Dispatchers.IO) {
        fun updateFileState(starredStarred: Starred) {
            response?.indexOfFirst {
                if (it is FilesDataModel.File) {
                    it.driveFile.id == file.id
                } else false
            }?.let { index ->
                if (index != -1) {
                    val newFile = FilesDataModel.File(
                        file.copy(starred = starredStarred)
                    )
                    response!![index] = newFile
                    _filesList.postValue(Resource.Success(response!!))
                }
            }
        }

        try {
            updateFileState(Starred.LOADING)

            val update = driveRepository.updateFile(
                fileId = file.id,
                starred = starred
            )
            when (update) {
                is Resource.Error -> {
                    _fileUpdate.emit(update.message!!)
                    updateFileState(if (starred) Starred.UNSTARRED else Starred.STARRED)
                }
                is Resource.Success -> {
                    Log.d(TAG, "File updated")
                    updateFileState(
                        if (starred) Starred.STARRED else Starred.UNSTARRED
                    )
                }
                else -> {}
            }
        } catch (cancel: CancellationException) {
            updateFileState(if (starred) Starred.UNSTARRED else Starred.STARRED)
            _fileUpdate.emit("Unable to update file")
            Log.d(TAG, cancel.message ?: "CancellationException")
        } catch (timeout: SocketTimeoutException) {
            updateFileState(if (starred) Starred.UNSTARRED else Starred.STARRED)
            _fileUpdate.emit("Server timed out")
            Log.d(TAG, timeout.message ?: "SocketTimeoutException")
        } catch (e: Exception) {
            updateFileState(if (starred) Starred.UNSTARRED else Starred.STARRED)
            _fileUpdate.emit(e.message ?: "Something went wrong")
            Log.e(TAG, "Something went wrong", e)
        }
    }

}
