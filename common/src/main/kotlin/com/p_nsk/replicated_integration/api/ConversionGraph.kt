package com.p_nsk.replicated_integration.api

class ConversionGraph(
    val conversions: List<MatterConversion>,
) {
    val byOutputsNode: Map<MatterNodeKey, List<MatterConversion>> =
        conversions
            .flatMap { conversion ->
                conversion.produces.map { produced ->
                    produced.node to conversion
                }
            }
            .groupBy({ it.first }, { it.second })

    @Suppress("unused")
    val byInputsNode: Map<MatterNodeKey, List<MatterConversion>> =
        conversions
            .flatMap { conversion ->
                conversion.consumes.map { consumed ->
                    consumed.node to conversion
                }
            }
            .groupBy({ it.first }, { it.second })
}
