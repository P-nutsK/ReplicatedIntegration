package com.p_nsk.replicated_integration.api

class SimpleConversionSolver(
    private val maxIterations: Int = 256,
) {
    fun solve(
        graph: ConversionGraph,
        defaults: Map<MatterNodeKey, LiteMatterCompound>,
    ): Map<MatterNodeKey, LiteMatterCompound> {
        val known = defaults.toMutableMap()

        repeat(maxIterations) {
            var changed = false

            for (conversion in graph.conversions) {
                if (conversion.produces.size != 1) continue

                val consumedValue = evaluateConsumes(conversion, known) ?: continue
                val produced = conversion.produces.single()

                val candidate = consumedValue.divide(produced.amount.toDouble())

                val old = known[produced.node]
                if (old == null || candidate.isBetterThan(old)) {
                    known[produced.node] = candidate
                    changed = true
                }
            }

            if (!changed) return known
        }

        return known
    }

    private fun evaluateConsumes(
        conversion: MatterConversion,
        known: Map<MatterNodeKey, LiteMatterCompound>,
    ): LiteMatterCompound? {
        var result = LiteMatterCompound(emptyMap())

        for (consume in conversion.consumes) {
            val value = known[consume.node] ?: return null
            result = result.add(value.multiply(consume.amount.toDouble()))
        }

        return result
    }
}
