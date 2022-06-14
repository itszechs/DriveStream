package zechs.drive.stream.ui.files.adapter

import androidx.recyclerview.widget.DiffUtil
import zechs.drive.stream.data.model.DriveFile

class FilesItemDiffCallback : DiffUtil.ItemCallback<DriveFile>() {

    override fun areItemsTheSame(
        oldItem: DriveFile, newItem: DriveFile
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: DriveFile, newItem: DriveFile
    ) = oldItem == newItem

}