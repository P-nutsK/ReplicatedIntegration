package com.p_nsk.replicated_integration.api.graph

import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.MatterNodeKey

class ConversionGraph(
    val conversions: List<MatterConversion>,
) {
    val byOutputsNode: Map<MatterNodeKey, List<MatterConversion>> =
        conversions
            .map { conversion -> conversion.produces.node to conversion }
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
