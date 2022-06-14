package zechs.drive.stream.ui.files.adapter

import androidx.recyclerview.widget.RecyclerView
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.ItemDriveFileBinding

class FilesViewHolder(
    private val itemBinding: ItemDriveFileBinding,
    val filesAdapter: FilesAdapter
) : RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(item: DriveFile) {}
}