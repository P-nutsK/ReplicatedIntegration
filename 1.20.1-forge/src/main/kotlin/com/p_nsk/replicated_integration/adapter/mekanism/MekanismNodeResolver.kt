package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterAmount
import com.p_nsk.replicated_integration.api.MatterNodeKey
import com.p_nsk.replicated_integration.api.MatterNodes
import mekanism.api.chemical.ChemicalStack
import mekanism.api.chemical.gas.GasStack
import mekanism.api.chemical.infuse.InfusionStack
import mekanism.api.chemical.pigment.PigmentStack
import mekanism.api.chemical.slurry.SlurryStack
import net.minecraft.resources.ResourceLocation

object MekanismNodeResolver {
    fun chemicalNode(stack: ChemicalStack<*>): MatterNodeKey? {
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

    fun chemicalAmount(stack: ChemicalStack<*>): MatterAmount? =
        chemicalNode(stack)?.takeIf { stack.amount > 0L }?.let { MatterAmount(it, stack.amount) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
