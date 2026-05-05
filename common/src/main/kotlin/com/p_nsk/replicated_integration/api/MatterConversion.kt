package com.p_nsk.replicated_integration.api

@JvmRecord
data class MatterConversion(
    val id: LiteResourceLocation,
    val consumes: List<MatterAmount>,
    val produces: MatterAmount,
    val credits: List<MatterAmount> = emptyList(),
    // This is not general recipe metadata; it exists specifically so the solver can reject
    // re-entering the same reversible conversion pair within a single derivation chain.
    val loopGuardKey: LiteResourceLocation? = null,
) {
    init {
        require(consumes.isNotEmpty()) { "consumes must not be empty" }
    }
}
