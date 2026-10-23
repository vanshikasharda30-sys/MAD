package com.example.clinicai.ui.doctors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.databinding.ItemDateBinding
import com.example.clinicai.domain.DayItem

/** Horizontal strip of the next days a patient can book, like the website's date strip. */
class DateStripAdapter(
    private val onSelect: (DayItem) -> Unit,
) : ListAdapter<DayItem, DateStripAdapter.Holder>(Diff) {

    var selectedIso: String? = null
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(private val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(day: DayItem) {
            val context = binding.root.context
            binding.dateWeekday.text = if (day.isToday) context.getString(R.string.book_today) else day.weekday
            binding.dateDay.text = day.dayOfMonth.toString()
            binding.root.isSelected = day.iso == selectedIso
            binding.root.contentDescription = "${binding.dateWeekday.text} ${day.dayOfMonth}"
            binding.root.setOnClickListener { onSelect(day) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<DayItem>() {
        override fun areItemsTheSame(a: DayItem, b: DayItem) = a.iso == b.iso
        override fun areContentsTheSame(a: DayItem, b: DayItem) = a == b
    }
}
