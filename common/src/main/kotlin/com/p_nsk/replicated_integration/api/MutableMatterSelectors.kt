package com.p_nsk.replicated_integration.api

class MutableMatterSelectors {
    private val values = linkedMapOf<MatterSelectorKey, ExplicitMatterValue>()

    fun put(selector: MatterSelectorKey, compound: LiteMatterCompound, source: ExplicitMatterSource) {
        val current = values[selector]
        val candidate = ExplicitMatterValue.Set(compound, source)
        if (current == null || shouldReplace(current, candidate)) {
            values[selector] = candidate
        }
    }

    fun deny(selector: MatterSelectorKey, source: ExplicitMatterSource) {
        val current = values[selector]
        val candidate = ExplicitMatterValue.Deny(source)
        if (current == null || shouldReplace(current, candidate)) {
            values[selector] = candidate
        }
    }

    fun putAll(other: Map<MatterSelectorKey, ExplicitMatterValue>) {
        other.forEach { (selector, value) ->
            when (value) {
                is ExplicitMatterValue.Deny -> deny(selector, value.source)
                is ExplicitMatterValue.Set -> put(selector, value.compound, value.source)
            }
        }
    }

    fun snapshot(): Map<MatterSelectorKey, ExplicitMatterValue> = values.toMap()

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
