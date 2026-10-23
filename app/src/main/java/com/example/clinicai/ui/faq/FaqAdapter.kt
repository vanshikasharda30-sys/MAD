package com.example.clinicai.ui.faq

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.data.model.Faq
import com.example.clinicai.databinding.ItemFaqBinding
import com.example.clinicai.ui.common.show

/** Tap a question to expand its answer. */
class FaqAdapter : ListAdapter<Faq, FaqAdapter.Holder>(Diff) {

    private val expanded = mutableSetOf<String>()

    inner class Holder(private val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(faq: Faq) {
            binding.question.text = faq.question
            binding.answer.text = faq.answer
            val open = faq.id in expanded
            binding.answer.show(open)
            binding.chevron.rotation = if (open) 180f else 0f
            binding.root.setOnClickListener {
                if (!expanded.add(faq.id)) expanded.remove(faq.id)
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<Faq>() {
        override fun areItemsTheSame(a: Faq, b: Faq) = a.id == b.id
        override fun areContentsTheSame(a: Faq, b: Faq) = a == b
    }
}
