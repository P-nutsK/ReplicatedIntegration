package com.p_nsk.replicated_integration.api.selector

import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.NodeKey

object MatterSelectorMaterializer {
    fun materialize(
        selectors: Map<MatterSelectorKey, ExplicitMatterValue>,
        expandTag: (type: LiteResourceLocation, id: LiteResourceLocation) -> Iterable<NodeKey>,
    ): Map<NodeKey, ExplicitMatterValue> {
        val values = linkedMapOf<NodeKey, AppliedValue>()
        for ((selector, explicitValue) in selectors.entries.sortedBy { it.key }) {
            val targets =
                when (selector.kind) {
                    MatterSelectorKind.NODE -> listOf(NodeKey(selector.type, selector.id))
                    MatterSelectorKind.TAG -> expandTag(selector.type, selector.id).toList()
                }
            val specificity =
                when (selector.kind) {
                    MatterSelectorKind.NODE -> 1
                    MatterSelectorKind.TAG -> 0
                }
            for (target in targets) {
                val current = values[target]
                if (current == null || shouldReplace(current, explicitValue, specificity)) {
                    values[target] = AppliedValue(explicitValue, specificity)
                }
            }
        }
        return values.mapValues { it.value.explicitValue }
    }

    private fun shouldReplace(
        current: AppliedValue,
        candidate: ExplicitMatterValue,
        specificity: Int,
    ): Boolean {
        if (candidate.source.priority != current.explicitValue.source.priority) {
            return candidate.source.priority > current.explicitValue.source.priority
        }
        if (specificity != current.specificity) {
            return specificity > current.specificity
        }
        return when {
            current.explicitValue is ExplicitMatterValue.Deny -> false
            candidate is ExplicitMatterValue.Deny -> true
            current.explicitValue is ExplicitMatterValue.Set && candidate is ExplicitMatterValue.Set ->
                candidate.compound.isBetterThan(current.explicitValue.compound)
            else -> false
        }
    }

    private data class AppliedValue(
        val explicitValue: ExplicitMatterValue,
        val specificity: Int,
    )
}
