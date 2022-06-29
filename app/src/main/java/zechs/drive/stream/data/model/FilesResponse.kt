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
) {
    fun toDriveFile() = DriveFile(id, name, size, mimeType, iconLink)
}