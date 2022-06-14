package zechs.drive.stream.data.model

import androidx.annotation.Keep

@Keep
data class DriveFile(
    val id: String,
    val name: String,
    val size: Long?,
    val mimeType: String,
    val iconLink: String?
)