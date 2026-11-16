package io.github.threeinone.hook

internal object SystemIconPolicy {
    fun effectiveIntensity(intensity: Float, inTintArea: Boolean): Float =
        if (inTintArea) intensity.coerceIn(0f, 1f) else 0f

    // Limit exclusions to one SystemUI measure/layout call; never own its slot list.
    // ColorOS 17 exposes this field as java.util.Set, ColorOS 16 as java.util.List;
    // both support the addAll/removeAll/contains operations used here.
    class SlotExclusion(private val slots: MutableCollection<String>, excluded: Collection<String>) {
        private val added = excluded.distinct().filterNot { it in slots }
        init { slots.addAll(added) }
        fun restore() { slots.removeAll(added.toSet()) }
    }
}
