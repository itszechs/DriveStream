package zechs.drive.stream.ui.profile.adapter

import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import zechs.drive.stream.data.model.Account
import zechs.drive.stream.databinding.ItemTextBinding

class AccountsViewHolder(
    private val itemBinding: ItemTextBinding,
    val accountsAdapter: AccountsAdapter
) : RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(account: Account) {
        itemBinding.apply {
            textView.text = account.name
            root.setOnClickListener {
                accountsAdapter.onClickListener.invoke(account)
            }
            roundCheck.isGone = !account.isDefault
            btnMenu.setOnClickListener {
                accountsAdapter.onMenuClickListener.invoke(btnMenu, account)
            }
        }
    }

}