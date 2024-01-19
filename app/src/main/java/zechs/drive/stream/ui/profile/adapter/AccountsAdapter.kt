package zechs.drive.stream.ui.profile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import zechs.drive.stream.R
import zechs.drive.stream.data.model.Account
import zechs.drive.stream.databinding.ItemTextBinding

class AccountsAdapter(
    val onClickListener: (Account) -> Unit,
    val onMenuClickListener: (View, Account) -> Unit
) : ListAdapter<Account, AccountsViewHolder>(AccountItemDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ) = AccountsViewHolder(
        itemBinding = ItemTextBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ),
        accountsAdapter = this
    )

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int) {
        val item = getItem(position)
        return holder.bind(item)

    }

    override fun getItemViewType(
        position: Int
    ) = R.layout.item_text

}