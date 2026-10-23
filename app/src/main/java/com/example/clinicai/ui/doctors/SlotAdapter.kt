package com.example.clinicai.ui.doctors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.databinding.ItemSlotBinding
import com.example.clinicai.databinding.ItemSlotHeaderBinding
import com.example.clinicai.domain.DayPart
import com.example.clinicai.domain.SlotGroup
import com.example.clinicai.util.DateFormats

/** Time slots grouped under Morning / Afternoon / Evening headers. */
class SlotAdapter(
    private val onSelect: (String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {
        data class Header(val part: DayPart) : Item
        data class Slot(val time: String) : Item
    }

    private var items: List<Item> = emptyList()

    var selectedSlot: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun submit(groups: List<SlotGroup>) {
        items = groups.flatMap { g -> listOf<Item>(Item.Header(g.part)) + g.slots.map { Item.Slot(it) } }
        notifyDataSetChanged()
    }

    /** Headers span the whole grid row; slots take one cell. */
    fun spanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) = if (items[position] is Item.Header) spanCount else 1
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = if (items[position] is Item.Header) TYPE_HEADER else TYPE_SLOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemSlotHeaderBinding.inflate(inflater, parent, false))
        } else {
            SlotHolder(ItemSlotBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Header -> (holder as HeaderHolder).bind(item.part)
            is Item.Slot -> (holder as SlotHolder).bind(item.time)
        }
    }

    private inner class HeaderHolder(private val binding: ItemSlotHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(part: DayPart) {
            binding.slotHeader.setText(
                when (part) {
                    DayPart.MORNING -> R.string.book_morning
                    DayPart.AFTERNOON -> R.string.book_afternoon
                    DayPart.EVENING -> R.string.book_evening
                },
            )
        }
    }

    private inner class SlotHolder(private val binding: ItemSlotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(time: String) {
            binding.slotTime.text = DateFormats.time12h(time)
            binding.root.isSelected = time == selectedSlot
            binding.root.setOnClickListener { onSelect(time) }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SLOT = 1
    }
}
