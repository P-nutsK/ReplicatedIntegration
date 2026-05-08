package com.p_nsk.replicated_integration.bridge

import com.p_nsk.replicated_integration.api.graph.ConversionGraph
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.node.MatterNodeKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey

data class ForgeCalculationSnapshot(
    val defaultMatterRecipeCount: Int,
    val activeAddonIds: List<String>,
    val selectorSnapshot: Map<MatterSelectorKey, ExplicitMatterValue>,
    val explicitSnapshot: Map<MatterNodeKey, ExplicitMatterValue>,
    val graph: ConversionGraph,
)
