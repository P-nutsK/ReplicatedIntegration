package com.p_nsk.replicated_integration.api

class MutableMatterDefaults {
    private val values = linkedMapOf<MatterNodeKey, LiteMatterCompound>()

    fun put(node: MatterNodeKey, compound: LiteMatterCompound) {
        val current = values[node]
        if (current == null || compound.isBetterThan(current)) {
            values[node] = compound
        }
    }

    fun putAll(other: Map<MatterNodeKey, LiteMatterCompound>) {
        other.forEach(::put)
    }

    fun snapshot(): Map<MatterNodeKey, LiteMatterCompound> = values.toMap()
}
