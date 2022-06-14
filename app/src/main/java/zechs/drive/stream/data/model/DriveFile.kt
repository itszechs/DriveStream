package zechs.drive.stream.data.model

import androidx.annotation.Keep
import zechs.drive.stream.utils.util.Converter

@Keep
data class DriveFile(
    val id: String,
    val name: String,
    val size: Long?,
    val mimeType: String,
    val iconLink: String?
) {
    val humanSize = size?.let { Converter.toHumanSize(it) }

    val isVideoFile = mimeType.startsWith("video/")

    val isFolder = mimeType == "application/vnd.google-apps.folder"
            || mimeType == "drive#teamDrive"

    val isShortcut = mimeType == "application/vnd.google-apps.shortcut"

    val iconLink128 = iconLink?.replace("16", "128")
}