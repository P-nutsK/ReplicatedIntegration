package com.p_nsk.replicated_integration.api

class MutableMatterDefaults {
    private val values = linkedMapOf<MatterNodeKey, ExplicitMatterValue>()

    fun put(node: MatterNodeKey, compound: LiteMatterCompound, source: ExplicitMatterSource) {
        val current = values[node]
        val candidate = ExplicitMatterValue.Set(compound, source)
        if (current == null || shouldReplace(current, candidate)) {
            values[node] = candidate
        }
    }

    fun deny(node: MatterNodeKey, source: ExplicitMatterSource) {
        val current = values[node]
        val candidate = ExplicitMatterValue.Deny(source)
        if (current == null || shouldReplace(current, candidate)) {
            values[node] = candidate
        }
    }

    fun putAll(other: Map<MatterNodeKey, ExplicitMatterValue>) {
        other.forEach { (node, value) ->
            when (value) {
                is ExplicitMatterValue.Deny -> deny(node, value.source)
                is ExplicitMatterValue.Set -> put(node, value.compound, value.source)
            }
        }
    }

    fun snapshot(): Map<MatterNodeKey, ExplicitMatterValue> = values.toMap()

    private fun shouldReplace(
        current: ExplicitMatterValue,
        candidate: ExplicitMatterValue,
    ): Boolean {
        if (candidate.source.priority != current.source.priority) {
            return candidate.source.priority > current.source.priority
        }
        return when {
            current is ExplicitMatterValue.Deny -> false
            candidate is ExplicitMatterValue.Deny -> true
            current is ExplicitMatterValue.Set && candidate is ExplicitMatterValue.Set -> candidate.compound.isBetterThan(current.compound)
            else -> false
        }
    }
}
