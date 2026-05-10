package com.p_nsk.replicated_integration.adapter.mekanism

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.p_nsk.replicated_integration.core.TargetValidation
import mekanism.api.MekanismAPI
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.registries.IForgeRegistry

object MekanismMatterCommandInputs {
    fun gasSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions { MekanismAPI.gasRegistry().keys }

    fun gasValidator(): (ResourceLocation) -> TargetValidation =
        registryValidator("gas", MekanismAPI.gasRegistry())

    fun gasTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        tagSuggestions { MekanismAPI.gasRegistry() }

    fun gasTagValidator(): (ResourceLocation) -> TargetValidation =
        tagValidator("gas tag", MekanismAPI.gasRegistry())

    fun infuseTypeSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions { MekanismAPI.infuseTypeRegistry().keys }

    fun infuseTypeValidator(): (ResourceLocation) -> TargetValidation =
        registryValidator("infuse type", MekanismAPI.infuseTypeRegistry())

    fun infuseTypeTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        tagSuggestions { MekanismAPI.infuseTypeRegistry() }

    fun infuseTypeTagValidator(): (ResourceLocation) -> TargetValidation =
        tagValidator("infuse type tag", MekanismAPI.infuseTypeRegistry())

    fun pigmentSuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions { MekanismAPI.pigmentRegistry().keys }

    fun pigmentValidator(): (ResourceLocation) -> TargetValidation =
        registryValidator("pigment", MekanismAPI.pigmentRegistry())

    fun pigmentTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        tagSuggestions { MekanismAPI.pigmentRegistry() }

    fun pigmentTagValidator(): (ResourceLocation) -> TargetValidation =
        tagValidator("pigment tag", MekanismAPI.pigmentRegistry())

    fun slurrySuggestions(): SuggestionProvider<CommandSourceStack> =
        resourceSuggestions { MekanismAPI.slurryRegistry().keys }

    fun slurryValidator(): (ResourceLocation) -> TargetValidation =
        registryValidator("slurry", MekanismAPI.slurryRegistry())

    fun slurryTagSuggestions(): SuggestionProvider<CommandSourceStack> =
        tagSuggestions { MekanismAPI.slurryRegistry() }

    fun slurryTagValidator(): (ResourceLocation) -> TargetValidation =
        tagValidator("slurry tag", MekanismAPI.slurryRegistry())

    private fun <T> registryValidator(
        name: String,
        registry: IForgeRegistry<T>,
    ): (ResourceLocation) -> TargetValidation = { id ->
        if (registry.containsKey(id)) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown $name: $id"))
        }
    }

    private fun <T> tagValidator(
        name: String,
        registry: IForgeRegistry<T>,
    ): (ResourceLocation) -> TargetValidation = { id ->
        val tags = registry.tags()
        if (tags != null && tags.isKnownTagName(tags.createTagKey(id))) {
            TargetValidation.Valid
        } else {
            TargetValidation.Invalid(Component.literal("Unknown $name: #$id"))
        }
    }

    private fun resourceSuggestions(
        keys: () -> Iterable<ResourceLocation>,
    ): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            SharedSuggestionProvider.suggestResource(keys(), builder)
        }

    private fun <T> tagSuggestions(
        registry: () -> IForgeRegistry<T>,
    ): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { _, builder ->
            val tagNames =
                registry().tags()
                    ?.getTagNames()
                    ?.map { it.location() }
                    ?.toList()
                    ?: emptyList()

            SharedSuggestionProvider.suggestResource(tagNames, builder)
        }
}
