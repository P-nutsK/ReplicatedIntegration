package com.p_nsk.replicated_integration.adapter.mekanism

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.p_nsk.replicated_integration.core.TargetValidation
import mekanism.api.MekanismAPI
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

object MekanismMatterCommandInputs {
    fun chemicalSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions {
            MekanismAPI.CHEMICAL_REGISTRY.keySet()
        }

    @Suppress("DEPRECATION")
    fun chemicalValidator(): (ResourceLocation) -> TargetValidation = { id ->
        if (MekanismAPI.CHEMICAL_REGISTRY.containsKey(id) && id != MekanismAPI.EMPTY_CHEMICAL_NAME) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown chemical: $id"))
        }
    }

    fun chemicalTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions {
            MekanismAPI.CHEMICAL_REGISTRY.tags
                .map { it.first.location() }
                .toList()
        }

    fun chemicalTagValidator(): (ResourceLocation) -> TargetValidation = { id ->
        val exists = MekanismAPI.CHEMICAL_REGISTRY
            .getTag(TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, id))
            .map { holders -> holders.iterator().hasNext() }
            .orElse(false)

        if (exists) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown or empty chemical tag: #$id"))
        }
    }

    private fun resourceSuggestions(
        keys: () -> Iterable<ResourceLocation>,
    ): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            SharedSuggestionProvider.suggestResource(keys(), builder)
        }
}
