package zechs.drive.stream.ui.player2

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.R
import zechs.drive.stream.databinding.ActivityMpvBinding
import zechs.drive.stream.databinding.PlayerControlViewBinding
import zechs.drive.stream.ui.player.PlayerViewModel
import zechs.drive.stream.utils.util.Constants.Companion.DRIVE_API
import zechs.drive.stream.utils.util.Orientation
import zechs.drive.stream.utils.util.getNextOrientation
import zechs.drive.stream.utils.util.setOrientation
import zechs.mpv.MPVLib
import zechs.mpv.MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART
import zechs.mpv.MPVView
import zechs.mpv.utils.Utils
import kotlin.math.roundToInt

@AndroidEntryPoint
class MPVActivity : AppCompatActivity(), MPVLib.EventObserver {

    companion object {
        const val TAG = "MPVActivity"

        // fraction to which audio volume is ducked on loss of audio focus
        private const val AUDIO_FOCUS_DUCKING = 0.5f
        private const val SKIP_DURATION = 10 // in seconds
    }

    private lateinit var audioManager: AudioManager
    private var audioFocusRestore: () -> Unit = {}

    // View-binding
    private lateinit var binding: ActivityMpvBinding
    private lateinit var player: MPVView
    private lateinit var controller: PlayerControlViewBinding

    // ViewModel
    private val viewModel by viewModels<PlayerViewModel>()

    // States
    private var activityIsForeground = true
    private var userIsOperatingSeekbar = false
    private var controlsLocked = false

    // Configs
    private var onLoadCommands = mutableListOf<Array<String>>()
    private val speeds = arrayOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)
    private var orientation = Orientation.LANDSCAPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.copyAssets(this)

        binding = ActivityMpvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        player = binding.player
        controller = binding.controller

        controller
            .playerToolbar
            .setNavigationOnClickListener { finish() }

        player.initialize(filesDir.path)
        player.addObserver(this)
        playMedia()

        audioManager = getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
            audioFocusChangeListener,
            STREAM_MUSIC,
            AUDIOFOCUS_GAIN
        ).also {
            if (it != AUDIOFOCUS_REQUEST_GRANTED) {
                Log.w(TAG, "Audio focus not granted")
                onLoadCommands.add(arrayOf("set", "pause", "yes"))
            }
        }

        // setVolumeControlStream
        volumeControlStream = STREAM_MUSIC


        controller.apply {
            // setup controller view for mpv
            exoProgress.isVisible = false
            progressBar.isVisible = true

            // progress bar
            progressBar.setOnSeekBarChangeListener(seekBarChangeListener)

            // init onClick listeners
            btnPlayPause.setOnClickListener { player.cyclePause() }
            exoFfwd.setOnClickListener { skipForward() }
            exoRew.setOnClickListener { rewindBackward() }
            btnAudio.setOnClickListener { pickAudio() }
            btnSubtitle.setOnClickListener { pickSub() }
            btnChapter.setOnClickListener { pickChapter() }
            btnSpeed.setOnClickListener { pickSpeed() }
            btnResize.setOnClickListener { player.cycleScale() }

            btnRotate.setOnClickListener {
                orientation = getNextOrientation(orientation)
                Log.d(TAG, "orientation=${orientation}")
                setOrientation(this@MPVActivity, orientation)
            }

            btnLock.setOnClickListener {
                controlsLocked = true
                handleLockingControls()
            }

            btnUnlock.setOnClickListener {
                controlsLocked = false
                handleLockingControls()
            }

        }

        // hide/show controller
        player.setOnClickListener {
            val root = controller.root
            TransitionManager.beginDelayedTransition(root, Fade())
            root.isVisible = !root.isVisible
            if (root.isVisible) {
                handleLockingControls()
            }
        }

        updateOrientation(resources.configuration)

    }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser || controlsLocked)
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
    private val audioFocusChangeListener = OnAudioFocusChangeListener { type ->
        Log.v(TAG, "Audio focus changed: $type")
        when (type) {
            AUDIOFOCUS_LOSS,
            AUDIOFOCUS_LOSS_TRANSIENT -> {
                // loss can occur in addition to ducking, so remember the old callback
                val oldRestore = audioFocusRestore
                val wasPlayerPaused = player.paused ?: false
                player.paused = true
                audioFocusRestore = {
                    oldRestore()
                    if (!wasPlayerPaused) player.paused = false
                }
            }
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                MPVLib.command(arrayOf("multiply", "volume", AUDIO_FOCUS_DUCKING.toString()))
                audioFocusRestore = {
                    val inv = 1f / AUDIO_FOCUS_DUCKING
                    MPVLib.command(arrayOf("multiply", "volume", inv.toString()))
                }
            }
            AUDIOFOCUS_GAIN -> {
                audioFocusRestore()
                audioFocusRestore = {}
            }
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

        viewModel.getWatch(fileId)

        controller.playerToolbar.title = title

        val playUri = getStreamUrl(fileId)
        MPVLib.command(arrayOf("loadfile", playUri))
        player.play(playUri)

        MPVLib.setOptionString(
            "http-header-fields",
            "Authorization: Bearer $accessToken"
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startDuration.consumeAsFlow().collect { startPosition ->
                    resumeVideo(startPosition)
                }
            }
        }

    }

    private fun resumeVideo(startPosition: Long) {
        val startPositionInSeconds = startPosition / 1000
        Log.d(TAG, "MPV(resumeVideo=${Utils.prettyTime(startPositionInSeconds.toInt())})")
        MPVLib.setOptionString("start", Utils.prettyTime(startPositionInSeconds.toInt()))
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
            controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

        controller.btnPlayPause.icon = ContextCompat.getDrawable(
            /* context */ applicationContext,
            /* drawableId */ if (paused) R.drawable.ic_play_24
            else R.drawable.ic_pause_24
        )

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


    private fun updateOrientation(newConfig: Configuration) {
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                controller.btnRotate.apply {
                    orientation = Orientation.PORTRAIT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tooltipText = getString(R.string.landscape)
                    }
                    icon = ContextCompat.getDrawable(
                        /* context */ this@MPVActivity,
                        /* drawableId */ R.drawable.ic_landscape_24
                    )
                }
            }
            else -> {
                controller.btnRotate.apply {
                    orientation = Orientation.LANDSCAPE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tooltipText = getString(R.string.portrait)
                    }
                    icon = ContextCompat.getDrawable(
                        /* context */ this@MPVActivity,
                        /* drawableId */ R.drawable.ic_portrait_24
                    )
                }
            }
        }
    }


    private fun handleLockingControls() {
        TransitionManager.beginDelayedTransition(
            controller.root,
            AutoTransition().apply { duration = 150L }
        )
        if (controlsLocked) {
            lockControls()
        } else {
            unlockControls()
        }
    }

    private fun lockControls() {
        controller.apply {
            controlsScrollView.visibility = View.GONE
            mainControls.visibility = View.GONE
            btnUnlock.visibility = View.VISIBLE
            playerToolbar.visibility = View.GONE
        }
    }

    private fun unlockControls() {
        controller.apply {
            btnUnlock.visibility = View.GONE
            playerToolbar.visibility = View.VISIBLE
            controlsScrollView.visibility = View.VISIBLE
            mainControls.visibility = View.VISIBLE
        }
    }

    private fun saveProgress() {
        val watchedDuration = player.timePos ?: return
        val totalDuration = player.duration ?: return
        Log.d(TAG, "MPV(current=$watchedDuration, total=$totalDuration)")
        // convert these two values from seconds to milliseconds
        val watchedDurationInMills = (watchedDuration * 1000).toLong()
        val totalDurationInMills = (totalDuration * 1000).toLong()
        Log.d(TAG, "MPV(current=$watchedDurationInMills, total=$totalDurationInMills)")
        Log.d(TAG, "MPV(saveProgress=${Utils.prettyTime(watchedDuration)})")

        val watchProgress = (watchedDuration.toDouble() / totalDuration.toDouble()).toFloat() * 100
        if (watchProgress > 10) {
            val fileId = intent.getStringExtra("fileId")!!
            val title = intent.getStringExtra("title")!!
            viewModel.saveWatch(
                name = title,
                videoId = fileId,
                watchedDuration = watchedDurationInMills,
                totalDuration = totalDurationInMills
            )
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

    ////////////////    END OF MPV EVENTS    ////////////////


    override fun onPause() {
        saveProgress()

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation(newConfig)
    }

    override fun onDestroy() {
        Log.v(TAG, "Exiting.")

        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(audioFocusChangeListener)

        player.removeObserver(this)
        player.destroy()

        super.onDestroy()
    }

}