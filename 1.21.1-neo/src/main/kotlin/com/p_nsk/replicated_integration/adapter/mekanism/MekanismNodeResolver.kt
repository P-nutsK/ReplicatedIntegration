package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.node.NodeKey
import mekanism.api.chemical.ChemicalStack
import net.minecraft.resources.ResourceLocation

object MekanismNodeResolver {
    fun chemicalNode(stack: ChemicalStack): NodeKey? {
        if (stack.isEmpty) {
            return null
        }
        @Suppress("removal") val id = stack.typeRegistryName ?: return null
        return MatterNodes.chemical(id.toLite())
    }

    fun chemicalAmount(stack: ChemicalStack): NodeAmount? =
        chemicalNode(stack)?.takeIf { stack.amount > 0L }?.let { NodeAmount(it, stack.amount) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
