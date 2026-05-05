package com.p_nsk.replicated_integration.api

data class MatterAmount(val node: MatterNodeKey,val amount: Long) {
    init {
        require(amount > 0) { "amount must be positive" }
    }
}
