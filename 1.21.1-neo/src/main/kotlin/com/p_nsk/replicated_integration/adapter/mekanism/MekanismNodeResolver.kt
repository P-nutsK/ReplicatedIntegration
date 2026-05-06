package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.MatterAmount
import com.p_nsk.replicated_integration.api.node.MatterNodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodes
import mekanism.api.chemical.ChemicalStack
import net.minecraft.resources.ResourceLocation

object MekanismNodeResolver {
    fun chemicalNode(stack: ChemicalStack): MatterNodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = stack.typeRegistryName ?: return null
        return MatterNodes.chemical(id.toLite())
    }

    fun chemicalAmount(stack: ChemicalStack): MatterAmount? =
        chemicalNode(stack)?.takeIf { stack.amount > 0L }?.let { MatterAmount(it, stack.amount) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
