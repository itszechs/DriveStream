package zechs.drive.stream.data.model

import androidx.annotation.Keep
import zechs.drive.stream.utils.util.Converter

@Keep
data class DriveFile(
    var id: String,
    val name: String,
    val size: Long?,
    val mimeType: String,
    val iconLink: String?,
    val shortcutDetails: ShortcutDetails
) {
    val humanSize = size?.let { Converter.toHumanSize(it) }

    val isVideoFile = mimeType.startsWith("video/")

    val isFolder = mimeType == "application/vnd.google-apps.folder"
            || mimeType == "drive#drive"

    val isShortcut = mimeType == "application/vnd.google-apps.shortcut"

    val iconLink128 = iconLink?.replace("16", "128")
}