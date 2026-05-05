package com.p_nsk.replicated_integration.api

/**
 * MatterNodeTypeとidを保持するキー
 */
@JvmRecord
data class MatterNodeKey(val type: LiteResourceLocation, val id: LiteResourceLocation) : Comparable<MatterNodeKey> {

    override fun compareTo(other: MatterNodeKey): Int {
        return compareValuesBy(this, other, MatterNodeKey::type, MatterNodeKey::id)
    }

}
