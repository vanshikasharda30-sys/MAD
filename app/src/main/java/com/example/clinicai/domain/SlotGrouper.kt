package com.example.clinicai.domain

enum class DayPart { MORNING, AFTERNOON, EVENING }

data class SlotGroup(val part: DayPart, val slots: List<String>)

/** Mirrors the website's grouping: before 12:00 morning, before 17:00 afternoon, otherwise evening. */
object SlotGrouper {
    fun group(slots: List<String>): List<SlotGroup> {
        val buckets = DayPart.entries.associateWith { mutableListOf<String>() }
        for (slot in slots) {
            val hour = slot.substringBefore(":").toIntOrNull() ?: continue
            val part = when {
                hour < 12 -> DayPart.MORNING
                hour < 17 -> DayPart.AFTERNOON
                else -> DayPart.EVENING
            }
            buckets.getValue(part).add(slot)
        }
        return DayPart.entries.mapNotNull { part ->
            buckets.getValue(part).takeIf { it.isNotEmpty() }?.let { SlotGroup(part, it) }
        }
    }
}
