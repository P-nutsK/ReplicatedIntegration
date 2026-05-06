package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

data class MatterNodeTypeDef(
    val id: LiteResourceLocation,
    val displayName: String,
    val formatter: (LiteResourceLocation) -> String = LiteResourceLocation::toString,
)
