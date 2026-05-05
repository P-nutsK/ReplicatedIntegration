package com.p_nsk.replicated_integration.api

data class MatterNodeTypeDef(
    val id: LiteResourceLocation,
    val displayName: String,
    val formatter: (LiteResourceLocation) -> String = LiteResourceLocation::toString,
)
