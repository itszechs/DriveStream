package zechs.mpv

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import zechs.mpv.MPVLib.mpvFormat.*
import java.util.*
import kotlin.reflect.KProperty

class MPVView(
    context: Context,
    attrs: AttributeSet
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    companion object {
        internal const val TAG = "mpv"
    }

    fun initialize(configDir: String) {
        MPVLib.create(this.context)
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)

        initOptions(configDir)
        MPVLib.init()

        holder.addCallback(this)
        observeProperties()
    }

    private fun initOptions(configDir: String) {

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display!!
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        }
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.mode.refreshRate
        } else 60.0F
        val deviceLanguage = Locale.getDefault().isO3Language

        Log.d(TAG, "Device language: $deviceLanguage")
        Log.d(TAG, "Display ${display.displayId} reports FPS of $refreshRate")

        MPVLib.setOptionString("alang", deviceLanguage)
        MPVLib.setOptionString("override-display-fps", refreshRate.toString())
        MPVLib.setOptionString("video-sync", "audio")
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("hwdec", "mediacodec-copy")
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9")
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("tls-verify", "yes")
        MPVLib.setOptionString("tls-ca-file", "${configDir}/cacert.pem")
        MPVLib.setOptionString("input-default-bindings", "yes")
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("force-window", "no")

        // Limit de-muxer cache since the defaults are too high for mobile devices
        val cacheSize = "${64 * 1024 * 1024}"
        MPVLib.setOptionString("demuxer-max-bytes", cacheSize)
        MPVLib.setOptionString("demuxer-max-back-bytes", cacheSize)

        // DR is known to ruin performance at least on Exynos devices
        // refer to https://github.com/mpv-android/mpv-android/issues/508
        MPVLib.setOptionString("vd-lavc-dr", "no")
    }

    fun play(path: String) {
        this.playUri = path
    }

    // Called when back button is pressed, or app is shutting down
    fun destroy() {
        // Disable surface callbacks to avoid using uninitialized mpv state
        holder.removeCallback(this)
        MPVLib.destroy()
    }

    private fun observeProperties() {
        // This observes all properties needed by MPVView, MPVActivity or other classes
        data class Property(
            val name: String,
            val format: Int = MPV_FORMAT_NONE
        )

        val properties = arrayOf(
            Property("time-pos", MPV_FORMAT_INT64),
            Property("duration", MPV_FORMAT_INT64),
            Property("pause", MPV_FORMAT_FLAG),
            Property("track-list"),
            Property("video-params"),
            Property("video-format"),
        )

        properties.forEach { (name, format) ->
            MPVLib.observeProperty(name, format)
        }
    }

    fun addObserver(eventObserver: MPVLib.EventObserver) {
        MPVLib.addObserver(eventObserver)
    }

    fun removeObserver(eventObserver: MPVLib.EventObserver) {
        MPVLib.removeObserver(eventObserver)
    }

    data class Track(
        val mpvId: Int,
        val name: String
    )

    var tracks = mapOf<String, MutableList<Track>>(
        "audio" to arrayListOf(),
        "video" to arrayListOf(),
        "sub" to arrayListOf()
    )

    fun loadTracks() {
        for (list in tracks.values) {
            list.clear()
            // pseudo-track to allow disabling audio/subs
            list.add(Track(-1, context.getString(R.string.track_off)))
        }
        val count = MPVLib.getPropertyInt("track-list/count")!!
        // Note that because events are async, properties might disappear at any moment
        // so use ?: continue instead of !!
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
            if (!tracks.containsKey(type)) {
                Log.w(TAG, "Got unknown track type: $type")
                continue
            }
            val mpvId = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
            val lang = MPVLib.getPropertyString("track-list/$i/lang")
            val title = MPVLib.getPropertyString("track-list/$i/title")

            val trackName = if (!lang.isNullOrEmpty() && !title.isNullOrEmpty()) {
                context.getString(R.string.ui_track_title_lang, mpvId, title, lang)
            } else if (!lang.isNullOrEmpty() || !title.isNullOrEmpty()) {
                context.getString(R.string.ui_track_text, mpvId, (lang ?: "") + (title ?: ""))
            } else {
                context.getString(R.string.ui_track, mpvId)
            }

            tracks.getValue(type).add(
                Track(mpvId = mpvId, name = trackName)
            )
        }
    }

    data class Chapter(
        val index: Int,
        val title: String?,
        val time: Double
    )

    fun loadChapters(): MutableList<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val count = MPVLib.getPropertyInt("chapter-list/count")!!
        for (i in 0 until count) {
            val title = MPVLib.getPropertyString("chapter-list/$i/title")
            val time = MPVLib.getPropertyDouble("chapter-list/$i/time")!!
            chapters.add(
                Chapter(index = i, title = title, time = time)
            )
        }
        return chapters
    }

    private var playUri: String? = null

    // Property getters/setters

    var paused: Boolean?
        get() = MPVLib.getPropertyBoolean("pause")
        set(paused) = MPVLib.setPropertyBoolean("pause", paused!!)

    var timePos: Int?
        get() = MPVLib.getPropertyInt("time-pos")
        set(progress) = MPVLib.setPropertyInt("time-pos", progress!!)

    // no setter only getter
    var duration: Int?
        get() = MPVLib.getPropertyInt("duration")
        private set(duration) {
            // do nothing
        }

    class TrackDelegate {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = MPVLib.getPropertyString(property.name)
            // we can get null here for "no" or other invalid value
            return v?.toIntOrNull() ?: -1
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            if (value == -1)
                MPVLib.setPropertyString(property.name, "no")
            else
                MPVLib.setPropertyInt(property.name, value)
        }
    }

    // video id
    // var vid: Int by TrackDelegate()

    // audio id
    var aid: Int by TrackDelegate()

    // subtitle id
    var sid: Int by TrackDelegate()

    // Commands

    fun cyclePause() = MPVLib.command(arrayOf("cycle", "pause"))
    fun cycleScale() = MPVLib.command(arrayOf("cycle-values", "panscan", "1.0", "0.0"))

    // Surface callbacks
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.w(TAG, "attaching surface")
        MPVLib.attachSurface(holder.surface)
        // This forces mpv to render subs/osd/whatever into our surface even if it would ordinarily not
        MPVLib.setOptionString("force-window", "yes")

        if (playUri != null) {
            MPVLib.command(arrayOf("loadfile", playUri!!))
            playUri = null
        } else {
            // We disable video output when the context disappears, enable it back
            MPVLib.setPropertyString("vo", "gpu")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.w(TAG, "detaching surface")
        MPVLib.setPropertyString("vo", "null")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.detachSurface()
    }
}
