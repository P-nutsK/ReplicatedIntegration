package com.p_nsk.replicated_integration.api.model

import com.p_nsk.replicated_integration.api.node.NodeKey
/**
 * Amount付きのNode。NodeはAEKeyみたいなもの。
 * */
data class NodeAmount(val node: NodeKey, val amount: Long) {
    init {
        require(amount > 0) { "amount must be positive" }
    }
}
