package zechs.drive.stream.utils.util

import java.text.SimpleDateFormat
import java.util.Locale

object Converter {

    fun toHumanSize(size: Long): String {
        val kb = size.toString().toDouble() / 1024
        val mb = kb / 1024
        val gb = mb / 1024
        val tb = gb / 1024
        return when {
            size < 1024L -> "$size Bytes"
            size < 1024L * 1024 -> String.format("%.2f", kb) + " KB"
            size < 1024L * 1024 * 1024 -> String.format("%.2f", mb) + " MB"
            size < 1024L * 1024 * 1024 * 1024 -> String.format("%.2f", gb) + " GB"
            else -> String.format("%.2f", tb) + " TB"
        }
    }

    fun fromTimeInMills(
        time: Long,
        format: String = "hh:mm a dd MMM, yyyy",
    ): String {
        val date = java.util.Date(time)
        val formatter = SimpleDateFormat(format, Locale.ENGLISH)
        return formatter.format(date)
    }

}