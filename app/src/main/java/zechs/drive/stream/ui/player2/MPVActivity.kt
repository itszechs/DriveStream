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
import zechs.drive.stream.databinding.ActivityMpvBinding
import zechs.drive.stream.databinding.PlayerControlViewBinding
import zechs.drive.stream.utils.util.Constants.Companion.DRIVE_API
import zechs.mpv.MPVLib
import zechs.mpv.MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART
import zechs.mpv.MPVView
import zechs.mpv.utils.Utils


class MPVActivity : AppCompatActivity(), MPVLib.EventObserver {

    companion object {
        const val TAG = "MPVActivity"
    }

    // States
    private var activityIsForeground = true
    private var userIsOperatingSeekbar = false

    // View-binding
    private lateinit var binding: ActivityMpvBinding
    private lateinit var player: MPVView
    private lateinit var controller: PlayerControlViewBinding

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