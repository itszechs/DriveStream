package zechs.mpv.utils

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import zechs.mpv.MPVView.Companion.TAG
import java.io.*
import kotlin.math.abs

object Utils {

    fun copyAssets(context: Context) {
        val assetManager = context.assets
        val files = arrayOf("subfont.ttf", "cacert.pem")
        val configDir = context.filesDir.path
        for (filename in files) {
            var ins: InputStream? = null
            var out: OutputStream? = null
            try {
                ins = assetManager.open(filename, AssetManager.ACCESS_STREAMING)
                val outFile = File("$configDir/$filename")
                // Note that .available() officially returns an *estimated* number of bytes available
                // this is only true for generic streams, asset streams return the full file size
                if (outFile.length() == ins.available().toLong()) {
                    Log.v(TAG, "Skipping copy of asset file (exists same size): $filename")
                    continue
                }
                out = FileOutputStream(outFile)
                ins.copyTo(out)
                Log.w(TAG, "Copied asset file: $filename")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy asset file: $filename", e)
            } finally {
                ins?.close()
                out?.close()
            }
        }
    }

    fun prettyTime(d: Int, sign: Boolean = false): String {
        if (sign)
            return (if (d >= 0) "+" else "-") + prettyTime(abs(d))

        val hours = d / 3600
        val minutes = d % 3600 / 60
        val seconds = d % 60
        if (hours == 0)
            return "%02d:%02d".format(minutes, seconds)
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

}
