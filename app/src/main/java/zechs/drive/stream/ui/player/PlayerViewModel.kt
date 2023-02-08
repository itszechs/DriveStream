package zechs.drive.stream.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.data.model.WatchList
import zechs.drive.stream.data.repository.WatchListRepository
import zechs.drive.stream.ui.player.PlayerActivity.Companion.TAG
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val watchListRepository: WatchListRepository
) : ViewModel() {

    private val _startDuration = MutableSharedFlow<Long>()
    val startDuration = _startDuration.asSharedFlow()

    fun getWatch(videoId: String) = viewModelScope.launch(Dispatchers.IO) {
        val video = watchListRepository.getWatch(videoId)
        if (video == null) {
            Log.d(TAG, "Video not found in database")
        } else {
            Log.d(TAG, "Starting at ${video.watchedDuration}")
            _startDuration.emit(video.watchedDuration)
        }
    }

    fun saveWatch(
        name: String,
        videoId: String,
        watchedDuration: Long,
        totalDuration: Long,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val lookUpWatched = watchListRepository.getWatch(videoId)

        val watch = lookUpWatched?.copy(
            name = name,
            videoId = videoId,
            watchedDuration = watchedDuration,
            totalDuration = totalDuration
        ) ?: WatchList(
            name, videoId,
            watchedDuration, totalDuration
        )

        if (watch.hasFinished()) {
            Log.d(TAG, "Video has finished, removing from database")
            watchListRepository.deleteWatch(watch)
        } else {
            Log.d(TAG, "Saving video at ${watch.watchedDuration}")
            watchListRepository.insertWatch(watch)
        }
    }

}