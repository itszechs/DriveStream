package zechs.drive.stream.ui.files.adapter

import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.request.RequestOptions
import zechs.drive.stream.R
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.ItemDriveFileBinding
import zechs.drive.stream.databinding.ItemLoadingBinding
import zechs.drive.stream.utils.GlideApp

sealed class FilesViewHolder(
    binding: ViewBinding
) : RecyclerView.ViewHolder(binding.root) {


    class DriveFileViewHolder(
        private val itemBinding: ItemDriveFileBinding,
        val filesAdapter: FilesAdapter
    ) : FilesViewHolder(itemBinding) {

        private fun setStarred(file: DriveFile, isStarred: Boolean?) {
            if (isStarred == null) {
                itemBinding.btnStar.isGone = true
                return
            }
            itemBinding.btnStar.apply {
                setImageResource(
                    if (isStarred) {
                        R.drawable.ic_starred_round_24
                    } else R.drawable.ic_star_round_24
                )
                setOnClickListener {
                    filesAdapter.onStarClickListener.invoke(file, !isStarred)
                }
            }
        }

        fun bind(file: FilesDataModel.File) {
            val item = file.driveFile
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

                setStarred(item, item.starred)
            }
        }
    }

    class LoadingViewHolder(
        itemBinding: ItemLoadingBinding
    ) : FilesViewHolder(itemBinding)

}