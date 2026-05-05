package com.p_nsk.replicated_integration.addon

import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterAmount
import com.p_nsk.replicated_integration.api.MatterNodeKey
import com.p_nsk.replicated_integration.api.MatterNodes
import mekanism.api.chemical.ChemicalStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

object MekanismNodeResolver {
    fun itemNode(stack: ItemStack): MatterNodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = BuiltInRegistries.ITEM.getKey(stack.item)
        return MatterNodes.item(id.toLite())
    }

    fun fluidNode(stack: FluidStack): MatterNodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = BuiltInRegistries.FLUID.getKey(stack.fluid)
        return MatterNodes.fluid(id.toLite())
    }

    fun chemicalNode(stack: ChemicalStack): MatterNodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = stack.typeRegistryName ?: return null
        return MatterNodes.chemical(id.toLite())
    }

    fun itemAmount(stack: ItemStack): MatterAmount? =
        itemNode(stack)?.let { MatterAmount(it, stack.count.coerceAtLeast(1).toLong()) }

    fun fluidAmount(stack: FluidStack): MatterAmount? =
        fluidNode(stack)?.takeIf { stack.amount > 0 }?.let { MatterAmount(it, stack.amount.toLong()) }

    fun chemicalAmount(stack: ChemicalStack): MatterAmount? =
        chemicalNode(stack)?.takeIf { stack.amount > 0L }?.let { MatterAmount(it, stack.amount) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
