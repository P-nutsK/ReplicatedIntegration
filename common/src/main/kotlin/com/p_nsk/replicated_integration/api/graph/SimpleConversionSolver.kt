package com.p_nsk.replicated_integration.api.graph

import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.MatterNodeKey

class SimpleConversionSolver(
    private val maxIterations: Int = 256,
) {
    fun solve(
        graph: ConversionGraph,
        explicitValues: Map<MatterNodeKey, ExplicitMatterValue>,
    ): Map<MatterNodeKey, LiteMatterCompound> {
        val denied = explicitValues
            .filterValues { it is ExplicitMatterValue.Deny }
            .keys
        val pinned = explicitValues
            .mapNotNull { (node, value) ->
                (value as? ExplicitMatterValue.Set)?.let { node to it.compound }
            }
            .toMap()

        // We intentionally keep one best candidate per node instead of a full proof tree.
        // That makes this a lightweight propagation solver rather than a complete craft graph
        // search: if the cheapest local candidate later becomes unusable, we do not retain the
        // second-best proof for fallback. loopGuardKey is only meant to break obvious reversible
        // loops such as Rotary, not to turn this solver into a general path-search engine.
        val known =
            pinned.mapValuesTo(linkedMapOf()) { (_, value) ->
                SolvedCandidate(value)
            }

        repeat(maxIterations) {
            var changed = false

            for (conversion in graph.conversions) {
                if (conversion.produces.node in denied || conversion.produces.node in pinned) {
                    continue
                }
                if (conversion.consumes.any { it.node in denied } || conversion.credits.any { it.node in denied }) {
                    continue
                }
                val consumedValue = evaluateConsumes(conversion, known) ?: continue
                val creditedValue = applyCredits(consumedValue, conversion, known)
                val produced = conversion.produces

                val candidate = creditedValue.divide(produced.amount.toDouble())
                val candidateGuards =
                    consumedValue.usedLoopGuards.let { used ->
                        conversion.loopGuardKey?.let(used::plus) ?: used
                    }

                val old = known[produced.node]
                if (old == null || candidate.isBetterThan(old.value)) {
                    known[produced.node] = SolvedCandidate(candidate, candidateGuards)
                    changed = true
                }
            }

            if (!changed) return known.mapValues { it.value.value }
        }

        return known.mapValues { it.value.value }
    }

    private fun evaluateConsumes(
        conversion: MatterConversion,
        known: Map<MatterNodeKey, SolvedCandidate>,
    ): EvaluatedConsumes? {
        var result = LiteMatterCompound.Companion.EMPTY
        var usedLoopGuards = emptySet<LiteResourceLocation>()

        for (consume in conversion.consumes) {
            val candidate = known[consume.node] ?: return null
            if (conversion.loopGuardKey != null && conversion.loopGuardKey in candidate.usedLoopGuards) {
                return null
            }
            result = result.add(candidate.value.multiply(consume.amount.toDouble()))
            usedLoopGuards = usedLoopGuards + candidate.usedLoopGuards
        }

        return EvaluatedConsumes(result, usedLoopGuards)
    }

    private fun applyCredits(
        consumedValue: EvaluatedConsumes,
        conversion: MatterConversion,
        known: Map<MatterNodeKey, SolvedCandidate>,
    ): LiteMatterCompound {
        var credited = consumedValue.value
        for (credit in conversion.credits) {
            val value = known[credit.node]?.value ?: continue
            credited = credited.subtract(value.multiply(credit.amount.toDouble()))
        }
        return credited
    }

    private data class SolvedCandidate(
        val value: LiteMatterCompound,
        val usedLoopGuards: Set<LiteResourceLocation> = emptySet(),
    )

    private data class EvaluatedConsumes(
        val value: LiteMatterCompound,
        val usedLoopGuards: Set<LiteResourceLocation>,
    )
}
