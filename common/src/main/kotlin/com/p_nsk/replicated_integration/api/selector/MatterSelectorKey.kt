package com.p_nsk.replicated_integration.api.selector

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

@JvmRecord
data class MatterSelectorKey(
    val selector: MatterSelectorKind,
    val type: LiteResourceLocation,
    val id: LiteResourceLocation,
) : Comparable<MatterSelectorKey> {
    override fun compareTo(other: MatterSelectorKey): Int =
        compareValuesBy(this, other, MatterSelectorKey::selector, MatterSelectorKey::type, MatterSelectorKey::id)
}
