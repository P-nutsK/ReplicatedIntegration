package com.p_nsk.replicated_integration.api

enum class ExplicitMatterSource(
    val priority: Int,
    val displayName: String,
) {
    NODE_VALUE(0, "replicated_integration node value"),
    DATAPACK(1, "replication matter_values"),
    RUNTIME(2, "runtime override"),
}

sealed interface ExplicitMatterValue {
    val source: ExplicitMatterSource

    data class Set(
        val compound: LiteMatterCompound,
        override val source: ExplicitMatterSource,
    ) : ExplicitMatterValue

    data class Deny(
        override val source: ExplicitMatterSource,
    ) : ExplicitMatterValue
}
