package zechs.drive.stream.ui.files.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.ItemDriveFileBinding

class FilesAdapter(
    val onClickListener: (DriveFile) -> Unit
) : ListAdapter<DriveFile, FilesViewHolder>(FilesItemDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ) = FilesViewHolder(
        itemBinding = ItemDriveFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ),
        filesAdapter = this
    )

    override fun onBindViewHolder(
        holder: FilesViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

}