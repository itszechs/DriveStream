package zechs.drive.stream.ui.player

import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
import com.google.android.exoplayer2.ExoPlaybackException.*
import com.google.android.exoplayer2.Format.NO_VALUE
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import zechs.drive.stream.R
import zechs.drive.stream.data.repository.DriveRepository
import zechs.drive.stream.databinding.ActivityPlayerBinding
import zechs.drive.stream.ui.player.utils.AuthenticatingDataSource
import zechs.drive.stream.ui.player.utils.BufferConfig
import zechs.drive.stream.ui.player.utils.CustomTrackNameProvider
import zechs.drive.stream.utils.SessionManager
import zechs.drive.stream.utils.util.Constants.Companion.DRIVE_API
import zechs.drive.stream.utils.util.Orientation
import zechs.drive.stream.utils.util.getNextOrientation
import zechs.drive.stream.utils.util.setOrientation
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt


@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val TAG = "PlayerActivity"
    }

    @Inject
    lateinit var driveRepository: Lazy<DriveRepository>

    @Inject
    lateinit var sessionManager: Lazy<SessionManager>

    // View binding
    private lateinit var binding: ActivityPlayerBinding

    // ViewModel
    private val viewModel by viewModels<PlayerViewModel>()

    // Exoplayer
    private lateinit var player: ExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var trackSelector: DefaultTrackSelector

    @Suppress("DEPRECATION")
    private lateinit var playerView: PlayerView

    // Player views
    private lateinit var mainControlsRoot: LinearLayout
    private lateinit var controlsScrollView: HorizontalScrollView
    private lateinit var progressViewGroup: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var btnAudio: MaterialButton
    private lateinit var btnSubtitle: MaterialButton
    private lateinit var btnChapter: MaterialButton
    private lateinit var btnResize: MaterialButton
    private lateinit var btnPip: MaterialButton
    private lateinit var btnSpeed: MaterialButton
    private lateinit var btnRotate: MaterialButton
    private lateinit var btnLock: MaterialButton
    private lateinit var btnUnlock: MaterialButton

    // Dialogs
    private var audioDialog: Dialog? = null
    private var subtitleDialog: Dialog? = null
    private var chapterDialog: Dialog? = null

    // States
    private var onStopCalled = false
    private var controlsLocked = false

    // Configs
    private var speed = arrayOf("0.25x", "0.5x", "Normal", "1.25x", "1.5x", "2x")
    private var orientation = Orientation.LANDSCAPE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        playerView = binding.playerView

        mainControlsRoot = playerView.findViewById(R.id.mainControls)
        controlsScrollView = playerView.findViewById(R.id.controlsScrollView)
        progressViewGroup = playerView.findViewById(R.id.linearLayout2)
        toolbar = playerView.findViewById(R.id.playerToolbar)
        btnPlayPause = playerView.findViewById(R.id.btnPlayPause)
        btnAudio = playerView.findViewById(R.id.btnAudio)
        btnSubtitle = playerView.findViewById(R.id.btnSubtitle)
        btnChapter = playerView.findViewById(R.id.btnChapter)
        btnResize = playerView.findViewById(R.id.btnResize)
        btnPip = playerView.findViewById(R.id.btnPip)
        btnSpeed = playerView.findViewById(R.id.btnSpeed)
        btnRotate = playerView.findViewById(R.id.btnRotate)
        btnLock = playerView.findViewById(R.id.btnLock)
        btnUnlock = playerView.findViewById(R.id.btnUnlock)

        // Back button
        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnAudio.setOnClickListener {
            if (audioDialog == null) {
                audioDialog = initPopupDialog(
                    btnAudio, getString(R.string.select_audio), C.TRACK_TYPE_AUDIO
                )
            }
            audioDialog?.show()
        }

        btnSubtitle.setOnClickListener {
            if (subtitleDialog == null) {
                subtitleDialog = initPopupDialog(
                    btnSubtitle, getString(R.string.select_subtitle), C.TRACK_TYPE_TEXT
                )
            }
            subtitleDialog?.show()
        }

        btnChapter.setOnClickListener {
            if (chapterDialog == null) {
                chapterDialog = initPopupDialog(
                    btnChapter, getString(R.string.chapters), C.TRACK_TYPE_METADATA
                )
            }
            chapterDialog?.show()
        }

        btnResize.setOnClickListener {
            TransitionManager.beginDelayedTransition(
                playerView, AutoTransition().apply {
                    interpolator = AccelerateInterpolator()
                    duration = 250
                }
            )
            playerView.apply {
                resizeMode = if (resizeMode == RESIZE_MODE_FIT) {
                    RESIZE_MODE_ZOOM
                } else RESIZE_MODE_FIT
            }
        }

        btnPip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPIPMode()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.pip_not_supported),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnSpeed.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(getString(R.string.select_speed))
                setItems(speed) { dialog, which ->
                    val param = when (which) {
                        0 -> PlaybackParameters(0.25f)  // 0.25x
                        1 -> PlaybackParameters(0.5f)   // 0.50x
                        2 -> PlaybackParameters(1.0f)   // 1.00x
                        3 -> PlaybackParameters(1.25f)   // 1.25x
                        4 -> PlaybackParameters(1.50f)   // 1.50x
                        5 -> PlaybackParameters(2.00f)   // 2.00x
                        else -> PlaybackParameters(1.0f)
                    }
                    Log.d(TAG, "Speed=${param.speed}")

                    player.playbackParameters = param
                    dialog.dismiss()
                    speedSnackbar(which)
                }
                this.show()
            }
        }

        btnRotate.setOnClickListener {
            orientation = getNextOrientation(orientation)
            Log.d(TAG, "orientation=${orientation}")
            setOrientation(this@PlayerActivity, orientation)
        }

        btnLock.setOnClickListener {
            controlsLocked = true
            handleLockingControls()
        }

        btnUnlock.setOnClickListener {
            controlsLocked = false
            handleLockingControls()
        }

        updateOrientation(resources.configuration)
        initPlayer()
        playMedia()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        playMedia()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPIPMode() {
        this.enterPictureInPictureMode(
            PictureInPictureParams
                .Builder()
                .build()
        )
    }

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Prevent screen from timing-out when video is playing
            playerView.keepScreenOn = when (playbackState) {
                Player.STATE_BUFFERING, Player.STATE_READY -> true
                else -> false
            }

            if (playbackState == Player.STATE_READY) {
                var subtitleText = ""

                player.videoFormat?.let {
                    if (it.width != NO_VALUE && it.height != NO_VALUE) {
                        subtitleText += "${it.width}x${it.height} - "
                    }
                    // frameRate can return NO_VALUE, which is a int
                    // can't compare it against float.
                    if (it.frameRate > 0) {
                        val rounded = (it.frameRate * 100.0).roundToInt() / 100.0
                        subtitleText += "${rounded}fps - "
                    }
                    it.codecs?.let { codec ->
                        subtitleText += codec
                    }
                }

                btnPlayPause.setOnClickListener {
                    player.playWhenReady = !player.playWhenReady
                }

                if (toolbar.title.isNullOrEmpty()) {
                    toolbar.title = player.mediaMetadata.title
                }

                if (toolbar.title.isNullOrEmpty()) {
                    // if `player.mediaMetadata.title` was empty
                    toolbar.title = getString(R.string.unknown)
                }

                toolbar.subtitle = subtitleText
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            TransitionManager.beginDelayedTransition(
                mainControlsRoot,
                AutoTransition().apply {
                    interpolator = AccelerateInterpolator()
                    duration = 150L
                }
            )
            btnPlayPause.icon = ContextCompat.getDrawable(
                /* context */ applicationContext,
                /* drawableId */ if (isPlaying) R.drawable.ic_pause_24
                else R.drawable.ic_play_24
            )
            Log.d(TAG, "isPlaying=${isPlaying}")
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            if (error is ExoPlaybackException) {
                showError(error)
            }
        }
    }

    private fun initPlayer() {
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)

        val rendererFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)

        // handles the duration of media to retain in the buffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                BufferConfig.MIN_BUFFER_DURATION,
                BufferConfig.MAX_BUFFER_DURATION,
                BufferConfig.MIN_PLAYBACK_START_BUFFER,
                BufferConfig.MIN_PLAYBACK_RESUME_BUFFER
            )
            .build()

        val deviceLanguage = Locale.getDefault().isO3Language
        Log.d(TAG, "Device language: $deviceLanguage")

        trackSelector = DefaultTrackSelector(this).apply {
            parameters = this.buildUponParameters()
                .setPreferredAudioLanguage(deviceLanguage)
                .build()
        }

        dataSourceFactory = DataSource.Factory {
            val dataSource = DefaultHttpDataSource.Factory()

            AuthenticatingDataSource
                .Factory(dataSource, driveRepository.get(), sessionManager.get())
                .createDataSource()
        }

        player = ExoPlayer.Builder(this, rendererFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    dataSourceFactory,
                    extractorsFactory
                )
            )
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(10_000)
            .build()

        playerView.setControllerVisibilityListener {
            if (it == View.VISIBLE) {
                handleLockingControls()
            }
        }

    }

    private fun handleLockingControls() {
        TransitionManager.beginDelayedTransition(
            playerView, AutoTransition().apply {
                duration = 150L
            }
        )
        if (controlsLocked) {
            lockControls()
        } else {
            unlockControls()
        }
    }

    private fun lockControls() {
        controlsScrollView.visibility = View.GONE
        mainControlsRoot.visibility = View.GONE
        btnUnlock.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
    }

    private fun unlockControls() {
        btnUnlock.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
        controlsScrollView.visibility = View.VISIBLE
        mainControlsRoot.visibility = View.VISIBLE
    }

    private fun releasePlayer() {
        player.removeListener(playerListener)
        player.clearMediaItems()
        player.release()
        playerView.player = null
    }

    private fun playMedia() {
        val fileId = intent.getStringExtra("fileId")

        toolbar.title = intent.getStringExtra("title")

        playerView.apply {
            player = this@PlayerActivity.player
            controllerHideOnTouch = true
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        if (fileId != null) {
            viewModel.getWatch(fileId)
            val mediaItem = MediaItem.Builder()
                .setUri(getStreamUrl(fileId))
                .build()

            player.apply {
                addListener(playerListener)
                setAudioAttributes(audioAttributes, true)
                addMediaItem(mediaItem)
                prepare()
            }.also { it.play() }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.startDuration.consumeAsFlow().collect { startPosition ->
                        resumeVideo(startPosition)
                    }
                }
            }

        }
    }

    private fun resumeVideo(startPosition: Long) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    player.seekTo(startPosition)
                    Log.d(TAG, "Seeking to $startPosition")
                    player.removeListener(this)
                }
            }
        })
    }


    private fun getStreamUrl(fileId: String): Uri {
        val uri = Uri.parse(
            "$DRIVE_API/files/${fileId}?supportsAllDrives=True&alt=media"
        )
        Log.d(TAG, "STREAM_URL=$uri")
        return uri
    }

    private fun updateOrientation(newConfig: Configuration) {
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                btnRotate.apply {
                    orientation = Orientation.PORTRAIT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tooltipText = getString(R.string.landscape)
                    }
                    icon = ContextCompat.getDrawable(
                        /* context */ this@PlayerActivity,
                        /* drawableId */ R.drawable.ic_landscape_24
                    )
                }
            }
            else -> {
                btnRotate.apply {
                    orientation = Orientation.LANDSCAPE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tooltipText = getString(R.string.portrait)
                    }
                    icon = ContextCompat.getDrawable(
                        /* context */ this@PlayerActivity,
                        /* drawableId */ R.drawable.ic_portrait_24
                    )
                }
            }
        }
    }

    private fun initPopupDialog(
        button: MaterialButton,
        label: String,
        trackType: @C.TrackType Int
    ): Dialog? {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        var renderer: Int? = null

        if (mappedTrackInfo == null) {
            return null
        }

        button.isVisible = true

        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (isRenderer(mappedTrackInfo, i, trackType)) {
                renderer = i
                break
            }
        }

        if (renderer == null) {
            button.isGone = true
            return null
        }

        val trackGroups = mutableListOf<Tracks.Group>()
        player.currentTracks.groups.forEach {
            if (it.type == trackType) {
                trackGroups.add(it)
            }
        }

        val callbacks = TrackSelectionDialogBuilder.DialogCallback { isDisabled, overrides ->
            val parametersBuilder = player.trackSelectionParameters
                .buildUpon()
                .apply {
                    setTrackTypeDisabled(trackType, isDisabled)
                    clearOverridesOfType(trackType)
                    overrides.values.forEach {
                        addOverride(it)
                    }
                }.build()
            player.trackSelectionParameters = parametersBuilder
        }

        return TrackSelectionDialogBuilder(
            /* context */this,
            label,
            trackGroups,
            callbacks
        ).also {
            it.setShowDisableOption(true)
            it.setTrackNameProvider(CustomTrackNameProvider(resources))
        }.build()
    }

    private fun isRenderer(
        mappedTrackInfo: MappedTrackInfo,
        rendererIndex: Int,
        trackType: @C.TrackType Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (trackGroupArray.length == 0) {
            return false
        }
        return trackType == mappedTrackInfo.getRendererType(rendererIndex)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun speedSnackbar(speedIndex: Int) {
        Snackbar.make(
            binding.playerView,
            "Playback speed set to ${speed[speedIndex]}",
            /* duration */ 750
        ).apply {
            anchorView = progressViewGroup
        }.also { it.show() }
    }

    private fun showError(error: ExoPlaybackException) {
        val errorGeneral = error.localizedMessage ?: getString(R.string.something_went_wrong)
        val errorDetailed = when (error.type) {
            TYPE_SOURCE -> error.sourceException.localizedMessage
            TYPE_RENDERER -> error.rendererException.localizedMessage
            TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
            TYPE_REMOTE -> errorGeneral
            else -> errorGeneral
        }
        errorSnackbar(errorGeneral, errorDetailed)
    }

    private fun errorSnackbar(textPrimary: String, textSecondary: String?) {
        Snackbar.make(playerView, textPrimary, Snackbar.LENGTH_LONG).apply {
            anchorView = progressViewGroup

            if (textSecondary != null) {
                setAction(getString(R.string.details)) {
                    MaterialAlertDialogBuilder(
                        this@PlayerActivity
                    ).apply {
                        setMessage(textSecondary)
                        setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        create()
                    }.also { it.show() }
                }
            }

        }.also { it.show() }
    }

    private fun saveProgress() {
        val watchedDuration = player.currentPosition
        val totalDuration = player.duration
        val watchProgress = (watchedDuration.toDouble() / totalDuration.toDouble()).toFloat() * 100
        if (watchProgress > 10) {
            val fileId = intent.getStringExtra("fileId")!!
            val title = intent.getStringExtra("title")!!
            viewModel.saveWatch(
                name = title,
                videoId = fileId,
                watchedDuration = watchedDuration,
                totalDuration = totalDuration
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerView.apply {
            controllerAutoShow = !isInPictureInPictureMode
            if (isInPictureInPictureMode) hideController() else showController()
        }
        if (onStopCalled) {
            saveProgress()
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation(newConfig)
    }

    override fun onPause() {
        saveProgress()
        super.onPause()
    }

    override fun onStop() {
        player.pause()
        super.onStop()
        onStopCalled = true
    }

    override fun onResume() {
        super.onResume()
        onStopCalled = false
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

}
