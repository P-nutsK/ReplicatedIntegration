package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodes
import mekanism.api.chemical.ChemicalStack
import mekanism.api.chemical.gas.GasStack
import mekanism.api.chemical.infuse.InfusionStack
import mekanism.api.chemical.pigment.PigmentStack
import mekanism.api.chemical.slurry.SlurryStack
import net.minecraft.resources.ResourceLocation

object MekanismNodeResolver {
    fun chemicalNode(stack: ChemicalStack<*>): NodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = stack.typeRegistryName ?: return null
        val key = id.toLite()
        return when (stack) {
            is GasStack -> MatterNodes.gas(key)
            is InfusionStack -> MatterNodes.infuseType(key)
            is PigmentStack -> MatterNodes.pigment(key)
            is SlurryStack -> MatterNodes.slurry(key)
            else -> null
        }
    }

    fun chemicalAmount(stack: ChemicalStack<*>): NodeAmount? =
        chemicalNode(stack)?.takeIf { stack.amount > 0L }?.let { NodeAmount(it, stack.amount) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
