package com.p_nsk.replicated_integration.api

@JvmRecord
data class MatterConversion(
    val id: LiteResourceLocation,
    val consumes: List<MatterAmount>,
    val produces: List<MatterAmount>,
) {
    init {
        require(consumes.isNotEmpty()) { "consumes must not be empty" }
        require(produces.isNotEmpty()) { "produces must not be empty" }
    }
}
