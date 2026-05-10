package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.node.NodeKey
import mekanism.api.MekanismAPI
import mekanism.api.chemical.ChemicalStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

object MekanismNodeResolver {
    @Suppress("DEPRECATION")
    fun chemicalNode(stack: ChemicalStack): NodeKey? {
        if (stack.isEmpty) {
            return null
        }
        @Suppress("removal") val id = stack.typeRegistryName ?: return null
        return MatterNodes.chemical(id.toLite())
    }

    fun chemicalAmount(stack: ChemicalStack): NodeAmount? =
        chemicalNode(stack)?.takeIf { stack.amount > 0L }?.let { NodeAmount(it, stack.amount) }

    @Suppress("DEPRECATION")
    fun chemicalNodesInTag(id: ResourceLocation): List<NodeKey> =
        MekanismAPI.CHEMICAL_REGISTRY.getTag(TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, id))
            .map { holders ->
                holders.mapNotNull { holder ->
                    val chemicalId = MekanismAPI.CHEMICAL_REGISTRY.getKey(holder.value())
                    if (chemicalId == MekanismAPI.EMPTY_CHEMICAL_NAME) null else MatterNodes.chemical(chemicalId.toLite())
                }
            }
            .orElse(emptyList())

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
