package com.p_nsk.replicated_integration.api.command

import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

object MatterCommandMutation {
    fun setSingleMatter(
        current: LiteMatterCompound,
        matterType: LiteResourceLocation,
        amount: Double,
    ): LiteMatterCompound {
        require(amount >= 0.0) { "amount must not be negative" }

        val values = current.values.toMutableMap()
        if (amount > 0.0) {
            values[matterType] = amount
        } else {
            values.remove(matterType)
        }
        return LiteMatterCompound(values)
    }
}
