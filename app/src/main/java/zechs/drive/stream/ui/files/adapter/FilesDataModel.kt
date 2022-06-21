package zechs.drive.stream.ui.files.adapter

import androidx.annotation.Keep
import zechs.drive.stream.data.model.DriveFile

sealed class FilesDataModel {

    @Keep
    data class File(
        val driveFile: DriveFile
    ) : FilesDataModel()

    object Loading : FilesDataModel()

}