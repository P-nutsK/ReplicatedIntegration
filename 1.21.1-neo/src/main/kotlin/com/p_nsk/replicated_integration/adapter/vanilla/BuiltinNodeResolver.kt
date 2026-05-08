package com.p_nsk.replicated_integration.adapter.vanilla

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodes
import net.minecraft.core.registries.Registries
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

object BuiltinNodeResolver {
    fun itemNode(stack: ItemStack): NodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = BuiltInRegistries.ITEM.getKey(stack.item)
        return MatterNodes.item(id.toLite())
    }

    fun itemNode(id: ResourceLocation): NodeKey =
        MatterNodes.item(id.toLite())

    fun fluidNode(stack: FluidStack): NodeKey? {
        if (stack.isEmpty) {
            return null
        }
        val id = BuiltInRegistries.FLUID.getKey(stack.fluid)
        return MatterNodes.fluid(id.toLite())
    }

    fun fluidNode(id: ResourceLocation): NodeKey? =
        BuiltInRegistries.FLUID.get(id)?.let { fluid ->
            if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) null else MatterNodes.fluid(id.toLite())
        }

    fun itemNodesInTag(id: ResourceLocation): List<NodeKey> =
        BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
            .map { holders ->
                holders.map { holder -> MatterNodes.item(BuiltInRegistries.ITEM.getKey(holder.value()).toLite()) }
            }
            .orElse(emptyList())

    fun fluidNodesInTag(id: ResourceLocation): List<NodeKey> =
        BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, id))
            .map { holders ->
                holders.mapNotNull { holder ->
                    val fluidId = BuiltInRegistries.FLUID.getKey(holder.value())
                    if (holder.value() == net.minecraft.world.level.material.Fluids.EMPTY) null else MatterNodes.fluid(fluidId.toLite())
                }
            }
            .orElse(emptyList())

    fun itemAmount(stack: ItemStack): NodeAmount? =
        itemNode(stack)?.let { NodeAmount(it, stack.count.coerceAtLeast(1).toLong()) }

    fun fluidAmount(stack: FluidStack): NodeAmount? =
        fluidNode(stack)?.takeIf { stack.amount > 0 }?.let { NodeAmount(it, stack.amount.toLong()) }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
