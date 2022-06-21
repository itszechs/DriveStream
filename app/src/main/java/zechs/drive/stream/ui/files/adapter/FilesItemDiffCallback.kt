package zechs.drive.stream.ui.files.adapter

import androidx.recyclerview.widget.DiffUtil

class FilesItemDiffCallback : DiffUtil.ItemCallback<FilesDataModel>() {


    override fun areItemsTheSame(
        oldItem: FilesDataModel,
        newItem: FilesDataModel
    ): Boolean = when {

        oldItem is FilesDataModel.Loading && newItem is FilesDataModel.Loading
                && oldItem == newItem
        -> true

        oldItem is FilesDataModel.File && newItem is FilesDataModel.File &&
                oldItem.driveFile.id == newItem.driveFile.id
        -> true

        else -> false
    }

    override fun areContentsTheSame(
        oldItem: FilesDataModel, newItem: FilesDataModel
    ) = oldItem == newItem

}