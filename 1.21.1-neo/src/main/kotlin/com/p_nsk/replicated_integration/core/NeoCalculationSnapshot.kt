package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.graph.ConversionGraph
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import net.minecraft.core.RegistryAccess

data class NeoCalculationSnapshot(
    val defaultMatterRecipeCount: Int,
    val activeAddonIds: List<String>,
    val selectorSnapshot: Map<MatterSelectorKey, ExplicitMatterValue>,
    val explicitSnapshot: Map<NodeKey, ExplicitMatterValue>,
    val graph: ConversionGraph,
    val registryAccess: RegistryAccess,
)
