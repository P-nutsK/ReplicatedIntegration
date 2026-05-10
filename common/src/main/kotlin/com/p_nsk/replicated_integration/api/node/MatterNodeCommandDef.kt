package com.p_nsk.replicated_integration.api.node

data class MatterNodeCommandDef(
    val literal: String,
    val supportsNodeSelector: Boolean = true,
    val supportsTagSelector: Boolean = false,
)
