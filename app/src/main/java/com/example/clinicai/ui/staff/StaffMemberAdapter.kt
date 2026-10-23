package com.example.clinicai.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.StaffMember
import com.example.clinicai.databinding.ItemStaffMemberBinding

class StaffMemberAdapter : ListAdapter<StaffMember, StaffMemberAdapter.Holder>(Diff) {

    class Holder(private val binding: ItemStaffMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(m: StaffMember) {
            val context = binding.root.context
            binding.memberName.text = m.name
            binding.memberEmail.text = m.email
            binding.memberRole.text = if (m.role == "ADMIN") context.getString(R.string.members_role_admin) else context.getString(R.string.members_role_reception)
            binding.memberStatus.setText(if (m.invitePending) R.string.members_pending else R.string.members_active)
            binding.memberStatus.setTextColor(ContextCompat.getColor(context, if (m.invitePending) R.color.amber else R.color.teal))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemStaffMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<StaffMember>() {
        override fun areItemsTheSame(a: StaffMember, b: StaffMember) = a.id == b.id
        override fun areContentsTheSame(a: StaffMember, b: StaffMember) = a == b
    }
}
