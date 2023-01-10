package zechs.drive.stream.data.model

import androidx.annotation.Keep

@Keep
data class FilesResponse(
    val files: List<File>,
    val nextPageToken: String?
)

@Keep
data class File(
    val id: String,
    val name: String,
    val size: Long?,
    val iconLink: String,
    val mimeType: String,
    val shortcutDetails: ShortcutDetails = ShortcutDetails()
) {
    fun toDriveFile() = DriveFile(id, name, size, mimeType, iconLink, shortcutDetails)
}

@Keep
data class ShortcutDetails(
    val targetId: String? = null,
    val targetMimeType: String? = null
)
