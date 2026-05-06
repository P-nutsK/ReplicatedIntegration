package com.p_nsk.replicated_integration.adapter.mekanism.synthetic

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.MatterAmount
import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.mekanism.synthetic.MekanismSyntheticConversionDefinitions

object ForgeMekanismSyntheticConversionContributor {
    fun collectNuclearConversions(collector: IConversionSink) {
        for (definition in MekanismSyntheticConversionDefinitions.nuclear) {
            collector.add(
                MatterConversion(
                    id = definition.id,
                    consumes = listOf(MatterAmount(MatterNodes.gas(definition.input), definition.inputAmount)),
                    produces = MatterAmount(MatterNodes.gas(definition.output), definition.outputAmount),
                ),
            )
        }
    }
}
