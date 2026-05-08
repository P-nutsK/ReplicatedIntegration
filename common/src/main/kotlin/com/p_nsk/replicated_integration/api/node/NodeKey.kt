package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

/**
 * AEKeyみたいな感じ
 */
@JvmRecord
data class NodeKey(val type: LiteResourceLocation, val id: LiteResourceLocation) : Comparable<NodeKey> {

    override fun compareTo(other: NodeKey): Int {
        return compareValuesBy(this, other, NodeKey::type, NodeKey::id)
    }

}
