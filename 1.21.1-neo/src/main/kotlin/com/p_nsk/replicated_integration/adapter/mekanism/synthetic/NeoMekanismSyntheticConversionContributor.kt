package com.p_nsk.replicated_integration.adapter.mekanism.synthetic

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.mekanism.synthetic.MekanismSyntheticConversionDefinitions

object NeoMekanismSyntheticConversionContributor {
    fun collectNuclearConversions(collector: IConversionSink) {
        for (definition in MekanismSyntheticConversionDefinitions.nuclear) {
            collector.add(
                MatterConversion(
                    id = definition.id,
                    consumes = listOf(NodeAmount(MatterNodes.chemical(definition.input), definition.inputAmount)),
                    produces = NodeAmount(MatterNodes.chemical(definition.output), definition.outputAmount),
                ),
            )
        }
    }
}
