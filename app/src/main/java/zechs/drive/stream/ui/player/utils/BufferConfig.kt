package zechs.drive.stream.ui.player.utils

class BufferConfig {

    companion object {
        //Min buffer while playing
        const val MIN_BUFFER_DURATION = 5000 // 50 secs

        //Max buffer while playback
        const val MAX_BUFFER_DURATION = 300_000 // 5 min

        //Min buffer before resuming playback
        const val MIN_PLAYBACK_START_BUFFER = 2000 // 2 sec

        //Min buffer when video is resumed
        const val MIN_PLAYBACK_RESUME_BUFFER = 2000 // 2 sec
    }

}