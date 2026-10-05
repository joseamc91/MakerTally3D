package com.joseamc91.makertally.ui

import com.joseamc91.makertally.domain.Filament
import com.joseamc91.makertally.domain.FilamentSort
import java.text.Collator
import java.util.Locale

/** Presentation order only: stored profiles and free-form variants remain unchanged. */
fun orderedFilaments(profiles: List<Filament>, mode: FilamentSort, language: String): List<Filament> {
    val collator = Collator.getInstance(Locale.forLanguageTag(language)).apply { strength = Collator.PRIMARY }
    val names = Comparator<Filament> { left, right -> collator.compare(left.displayName, right.displayName) }
    val prices = Comparator<Filament> { left, right ->
        // Compare the exact price/weight ratios without division, rounding or floating point.
        (left.purchasePrice * right.spoolWeight).compareTo(right.purchasePrice * left.spoolWeight)
    }
    val withinGroup = when (mode) {
        FilamentSort.Name -> names
        FilamentSort.PriceAscending -> prices.then(names)
        FilamentSort.PriceDescending -> prices.reversed().then(names)
    }.thenBy { it.id }
    return profiles.sortedWith(compareBy<Filament> { !it.active }.then(withinGroup))
}
