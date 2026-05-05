package com.p_nsk.replicated_integration.adapter.vanilla

import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterAmount
import com.p_nsk.replicated_integration.api.MatterNodeKey
import com.p_nsk.replicated_integration.api.MatterNodes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.registries.ForgeRegistries

object BuiltinNodeResolver {
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
        val id = ForgeRegistries.FLUIDS.getKey(stack.fluid) ?: return null
        return MatterNodes.fluid(id.toLite())
    }

    fun itemAmount(stack: ItemStack): MatterAmount? =
        itemNode(stack)?.let { MatterAmount(it, stack.count.coerceAtLeast(1).toLong()) }

    fun fluidAmount(stack: FluidStack): MatterAmount? =
        fluidNode(stack)?.takeIf { stack.amount > 0 }?.let { MatterAmount(it, stack.amount.toLong()) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
