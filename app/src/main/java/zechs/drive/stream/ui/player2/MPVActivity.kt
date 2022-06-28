package zechs.drive.stream.ui.player2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import zechs.drive.stream.R
import zechs.drive.stream.databinding.ActivityMpvBinding
import zechs.drive.stream.databinding.PlayerControlViewBinding
import zechs.drive.stream.utils.util.Constants.Companion.DRIVE_API
import zechs.mpv.MPVLib
import zechs.mpv.MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART
import zechs.mpv.MPVView
import zechs.mpv.utils.Utils
import kotlin.math.roundToInt


class MPVActivity : AppCompatActivity(), MPVLib.EventObserver {

    companion object {
        const val TAG = "MPVActivity"

        private const val SKIP_DURATION = 10 // in seconds
    }

    // States
    private var activityIsForeground = true
    private var userIsOperatingSeekbar = false

    // View-binding
    private lateinit var binding: ActivityMpvBinding
    private lateinit var player: MPVView
    private lateinit var controller: PlayerControlViewBinding

    // Configs
    private val speeds = arrayOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.copyAssets(this)

        binding = ActivityMpvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        player = binding.player
        controller = binding.controller

        player.initialize(filesDir.path)
        player.addObserver(this)
        playMedia()

        controller.apply {
            // setup controller view for mpv
            exoProgress.isVisible = false
            progressBar.isVisible = true

            // progress bar
            progressBar.setOnSeekBarChangeListener(seekBarChangeListener)

            // init onClick listeners
            btnPlay.setOnClickListener { player.cyclePause() }
            btnPause.setOnClickListener { player.cyclePause() }
            exoFfwd.setOnClickListener { skipForward() }
            exoRew.setOnClickListener { rewindBackward() }
            btnAudio.setOnClickListener { pickAudio() }
            btnSubtitle.setOnClickListener { pickSub() }
            btnChapter.setOnClickListener { pickChapter() }
            btnSpeed.setOnClickListener { pickSpeed() }
            btnResize.setOnClickListener { player.cycleScale() }
        }
    }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser)
                return
            player.timePos = progress
            updatePlaybackPos(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            userIsOperatingSeekbar = false
        }
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        playMedia()
    }

    private fun playMedia() {
        val fileId = intent.getStringExtra("fileId")
        val title = intent.getStringExtra("title")
        val accessToken = intent.getStringExtra("accessToken")

        if (fileId == null || accessToken == null) {
            Log.d(TAG, "FileId & AccessToken both are required. exiting...")
            finish()
            return
        }

        Log.d(TAG, "MPVActivity(fileId=$fileId, title=$title, accessToken=$accessToken)")

        val playUri = getStreamUrl(fileId)
        MPVLib.command(arrayOf("loadfile", playUri))
        player.play(playUri)

        MPVLib.setOptionString(
            "http-header-fields",
            "Authorization: Bearer $accessToken"
        )

    }

    private fun getStreamUrl(fileId: String): String {
        val uri = Uri.parse(
            "${DRIVE_API}/files/${fileId}?supportsAllDrives=True&alt=media"
        )
        Log.d(TAG, "STREAM_URL=$uri")
        return uri.toString()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun updatePlaybackPos(position: Int) {
        controller.exoPosition.text = Utils.prettyTime(position)
        if (!userIsOperatingSeekbar) {
            controller.progressBar.progress = position
        }
    }

    private fun updatePlaybackDuration(duration: Int) {
        controller.exoDuration.text = Utils.prettyTime(duration)
        if (!userIsOperatingSeekbar) {
            controller.progressBar.max = duration
        }
    }

    private fun updatePlaybackStatus(paused: Boolean) {
        TransitionManager.beginDelayedTransition(
            controller.mainControls,
            AutoTransition().apply { duration = 250L }
        )

        controller.btnPlay.isVisible = paused
        controller.btnPause.isVisible = !paused

        if (paused) {
            window.clearFlags(FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun skipForward() {
        val currentPos = player.timePos ?: return
        val newPos = currentPos + SKIP_DURATION
        player.timePos = newPos
    }

    private fun rewindBackward() {
        val currentPos = player.timePos ?: return
        val newPos = currentPos - SKIP_DURATION
        player.timePos = newPos
    }


    data class TrackData(
        val track_id: Int,
        val track_type: String
    )

    private fun trackSwitchNotification(f: () -> TrackData) {
        val (track_id, track_type) = f()
        val trackPrefix = when (track_type) {
            "audio" -> getString(R.string.audio)
            "sub" -> getString(R.string.subtitles)
            "video" -> getString(R.string.video)
            else -> "???"
        }

        if (track_id == -1) {
            configSnackbar("$trackPrefix ${getString(R.string.track_off)}")
            return
        }

        val trackName = player.tracks[track_type]
            ?.firstOrNull { it.mpvId == track_id }
            ?.name
            ?: "???"

        configSnackbar("$trackPrefix $trackName")
    }

    private fun pickAudio() {
        selectTrack(
            title = getString(R.string.select_audio),
            type = "audio",
            get = { player.aid },
            set = { player.aid = it }
        )
    }

    private fun pickSub() {
        selectTrack(
            title = getString(R.string.select_subtitle),
            type = "sub",
            get = { player.sid },
            set = { player.sid = it }
        )
    }

    private fun pickChapter() {
        val chapters = player.loadChapters()

        if (chapters.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.chapters))
                .setItems(arrayOf("None")) { dialog, _ ->
                    dialog.dismiss()
                }.show()
            return
        }

        val chapterArray = chapters.map {
            val timeCode = Utils.prettyTime(it.time.roundToInt())
            if (!it.title.isNullOrEmpty()) {
                getString(R.string.ui_chapter, it.title, timeCode)
            } else {
                getString(R.string.ui_chapter_fallback, it.index + 1, timeCode)
            }
        }.toTypedArray()

        val selectedIndex = MPVLib.getPropertyInt("chapter") ?: 0

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.chapters))
            .setSingleChoiceItems(chapterArray, selectedIndex) { dialog, item ->
                MPVLib.setPropertyInt("chapter", chapters[item].index)
                dialog.dismiss()
            }.show()

    }

    private fun selectTrack(title: String, type: String, get: () -> Int, set: (Int) -> Unit) {
        val tracks = player.tracks.getValue(type)
        val selectedMpvId = get()
        val selectedIndex = tracks.indexOfFirst { it.mpvId == selectedMpvId }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setSingleChoiceItems(
                tracks.map { it.name }.toTypedArray(),
                selectedIndex
            ) { dialog, item ->
                val trackId = tracks[item].mpvId
                set(trackId)
                dialog.dismiss()
                trackSwitchNotification { TrackData(trackId, type) }
            }
        }.also { it.show() }
    }

    private fun configSnackbar(msg: String, duration: Int = 750) {
        Snackbar.make(
            controller.root, msg, duration
        ).apply {
            anchorView = controller.linearLayout2
        }.also { it.show() }
    }

    private fun pickSpeed() {
        val currentSpeed = MPVLib.getPropertyDouble("speed")
        val selectedIndex = speeds.toList().indexOf(currentSpeed)

        Log.d(TAG, "currentSpeed=$currentSpeed")

        MaterialAlertDialogBuilder(this).apply {
            setTitle(getString(R.string.select_speed))
            setSingleChoiceItems(
                speeds.map { it.toString() }.toTypedArray(),
                selectedIndex
            ) { dialog, item ->
                setSpeed(speeds[item])
                dialog.dismiss()
                configSnackbar("Playback speed set to ${speeds[item]}x")
            }
        }.also { it.show() }
    }

    private fun setSpeed(speed: Double) {
        MPVLib.setPropertyDouble("speed", speed)
    }

    ////////////////    MPV EVENTS    ////////////////

    override fun eventProperty(property: String, value: Boolean) {
        if (activityIsForeground && property == "pause") {
            runOnUiThread { updatePlaybackStatus(value) }
        }
    }

    override fun eventProperty(property: String, value: Long) {
        if (!activityIsForeground) return
        runOnUiThread {
            when (property) {
                "time-pos" -> updatePlaybackPos(value.toInt())
                "duration" -> updatePlaybackDuration(value.toInt())
            }
        }
    }

    override fun eventProperty(property: String) {
        if (!activityIsForeground) return
        runOnUiThread { eventPropertyUi(property) }
    }

    override fun eventProperty(property: String, value: String) {
        if (!activityIsForeground) return
        runOnUiThread { eventPropertyUi(property) }
    }

    private fun eventPropertyUi(property: String) {
        if (activityIsForeground && property == "track-list") {
            player.loadTracks()
        }
    }

    override fun event(eventId: Int) {
        if (activityIsForeground && eventId == MPV_EVENT_PLAYBACK_RESTART) {
            runOnUiThread { updatePlaybackStatus(player.paused!!) }
        }
    }

    override fun onPause() {
        activityIsForeground = false

        if (isFinishing) {
            MPVLib.command(arrayOf("stop"))
        }
        super.onPause()
    }

    override fun onResume() {
        // If we weren't actually in the background
        // (e.g. multi window mode), don't reinitialize stuff
        if (activityIsForeground) {
            super.onResume()
            return
        }

        activityIsForeground = true
        super.onResume()
    }
}