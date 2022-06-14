package zechs.drive.stream.ui.files.adapter

import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import zechs.drive.stream.R
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.ItemDriveFileBinding
import zechs.drive.stream.utils.GlideApp

class FilesViewHolder(
    private val itemBinding: ItemDriveFileBinding,
    val filesAdapter: FilesAdapter
) : RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(item: DriveFile) {
        itemBinding.apply {

            val iconLink = item.iconLink128 ?: R.drawable.ic_my_drive_24

            GlideApp.with(ivFileType)
                .load(iconLink)
                .apply(RequestOptions().override(48, 48))
                .into(ivFileType)

            tvFileName.text = item.name

            val tvFileSizeTAG = "tvFileSize"

            tvFileSize.apply {
                tag = if (item.size == null) {
                    tvFileSizeTAG
                } else null

                isGone = tag == tvFileSizeTAG
                text = item.humanSize
            }

            root.setOnClickListener {
                filesAdapter.onClickListener.invoke(item)
            }

        }
    }
}