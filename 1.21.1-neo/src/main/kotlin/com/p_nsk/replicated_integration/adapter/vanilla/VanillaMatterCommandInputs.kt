package com.p_nsk.replicated_integration.adapter.vanilla

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.p_nsk.replicated_integration.core.TargetValidation
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.material.Fluids

object VanillaMatterCommandInputs {
    fun itemSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions {
            BuiltInRegistries.ITEM.keySet()
        }

    fun itemValidator(): (ResourceLocation) -> TargetValidation = { id ->
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown item: $id"))
        }
    }

    fun itemTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions {
            BuiltInRegistries.ITEM.tags
                .map { it.first.location() }
                .toList()
        }

    fun itemTagValidator(): (ResourceLocation) -> TargetValidation = { id ->
        val exists = BuiltInRegistries.ITEM
            .getTag(TagKey.create(Registries.ITEM, id))
            .map { holders -> holders.iterator().hasNext() }
            .orElse(false)

        if (exists) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown or empty item tag: #$id"))
        }
    }

    fun fluidSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions {
            BuiltInRegistries.FLUID.keySet()
        }

    fun fluidValidator(): (ResourceLocation) -> TargetValidation = { id ->
        val fluid = BuiltInRegistries.FLUID.get(id)
        if (fluid != Fluids.EMPTY) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown fluid: $id"))
        }
    }

    fun fluidTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions {
            BuiltInRegistries.FLUID.tags
                .map { it.first.location() }
                .toList()
        }

    fun fluidTagValidator(): (ResourceLocation) -> TargetValidation = { id ->
        val exists = BuiltInRegistries.FLUID
            .getTag(TagKey.create(Registries.FLUID, id))
            .map { holders -> holders.iterator().hasNext() }
            .orElse(false)

        if (exists) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown or empty fluid tag: #$id"))
        }
    }

    private fun resourceSuggestions(
        keys: () -> Iterable<ResourceLocation>,
    ): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            SharedSuggestionProvider.suggestResource(keys(), builder)
        }
}
