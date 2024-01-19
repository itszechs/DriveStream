package zechs.drive.stream.ui.profile.adapter

import androidx.recyclerview.widget.DiffUtil
import zechs.drive.stream.data.model.Account

class AccountItemDiffCallback : DiffUtil.ItemCallback<Account>() {

    override fun areItemsTheSame(
        oldItem: Account,
        newItem: Account
    ): Boolean = oldItem.name == newItem.name

    override fun areContentsTheSame(
        oldItem: Account, newItem: Account
    ) = oldItem == newItem

}