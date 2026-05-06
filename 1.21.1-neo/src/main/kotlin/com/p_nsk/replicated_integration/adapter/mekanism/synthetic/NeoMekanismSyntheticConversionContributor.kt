package com.p_nsk.replicated_integration.adapter.mekanism.synthetic

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.MatterAmount
import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.mekanism.synthetic.MekanismSyntheticConversionDefinitions

object NeoMekanismSyntheticConversionContributor {
    fun collectNuclearConversions(collector: IConversionSink) {
        for (definition in MekanismSyntheticConversionDefinitions.nuclear) {
            collector.add(
                MatterConversion(
                    id = definition.id,
                    consumes = listOf(MatterAmount(MatterNodes.chemical(definition.input), definition.inputAmount)),
                    produces = MatterAmount(MatterNodes.chemical(definition.output), definition.outputAmount),
                ),
            )
        }
    }
}
