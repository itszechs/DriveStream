package zechs.drive.stream.ui.files.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import zechs.drive.stream.R
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.ItemDriveFileBinding
import zechs.drive.stream.databinding.ItemLoadingBinding

class FilesAdapter(
    val onClickListener: (DriveFile) -> Unit,
    val onLongClickListener: (DriveFile) -> Unit,
    val onStarClickListener: (DriveFile, Boolean) -> Unit
) : ListAdapter<FilesDataModel, FilesViewHolder>(FilesItemDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): FilesViewHolder {

        val filesViewHolder = FilesViewHolder.DriveFileViewHolder(
            itemBinding = ItemDriveFileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ),
            filesAdapter = this
        )

        val loadingViewHolder = FilesViewHolder.LoadingViewHolder(
            itemBinding = ItemLoadingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )

        return when (viewType) {
            R.layout.item_loading -> loadingViewHolder
            R.layout.item_drive_file -> filesViewHolder
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: FilesViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is FilesViewHolder.LoadingViewHolder -> item as FilesDataModel.Loading
            is FilesViewHolder.DriveFileViewHolder -> holder.bind(item as FilesDataModel.File)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FilesDataModel.Loading -> R.layout.item_loading
            is FilesDataModel.File -> R.layout.item_drive_file
        }
    }
}