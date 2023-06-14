package zechs.drive.stream.ui.files.adapter

import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.request.RequestOptions
import zechs.drive.stream.R
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.data.model.Starred
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

        private fun setStarred(file: DriveFile, starredState: Starred) {
            when (starredState) {
                Starred.UNSTARRED -> {
                    itemBinding.apply {
                        btnStar.isInvisible = false
                        starLoading.isGone = true
                        btnStar.setImageResource(R.drawable.ic_star_round_24)
                        btnStar.setOnClickListener {
                            filesAdapter.onStarClickListener.invoke(file, true)
                        }
                    }
                }

                Starred.STARRED -> {
                    itemBinding.apply {
                        btnStar.isInvisible = false
                        starLoading.isGone = true
                        btnStar.setImageResource(R.drawable.ic_starred_round_24)
                        btnStar.setOnClickListener {
                            filesAdapter.onStarClickListener.invoke(file, false)
                        }
                    }
                }

                Starred.LOADING -> {
                    itemBinding.apply {
                        btnStar.isInvisible = true
                        starLoading.isGone = false
                    }
                }

                Starred.UNKNOWN -> {
                    itemBinding.apply {
                        btnStar.isGone = true
                        starLoading.isGone = true
                    }
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

                root.setOnLongClickListener {
                    filesAdapter.onLongClickListener.invoke(item)
                    return@setOnLongClickListener true
                }

                setStarred(item, item.starred)
            }
        }
    }

    class LoadingViewHolder(
        itemBinding: ItemLoadingBinding
    ) : FilesViewHolder(itemBinding)

}