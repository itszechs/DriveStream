package zechs.drive.stream.ui.player2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import zechs.drive.stream.databinding.ActivityMpvBinding
import zechs.drive.stream.utils.util.Constants.Companion.DRIVE_API
import zechs.mpv.MPVLib
import zechs.mpv.MPVView
import zechs.mpv.utils.Utils


class MPVActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MPVActivity"
    }

    // View-binding
    private lateinit var binding: ActivityMpvBinding
    private lateinit var player: MPVView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.copyAssets(this)

        binding = ActivityMpvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

        player = binding.player

        player.initialize(filesDir.path)
        playMedia()
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

}