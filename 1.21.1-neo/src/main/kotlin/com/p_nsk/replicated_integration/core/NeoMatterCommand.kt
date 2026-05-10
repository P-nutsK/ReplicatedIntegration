package com.p_nsk.replicated_integration.core

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterCommandDef
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

data class NeoMatterCommand(
    override val literal: String,
    override val nodeType: LiteResourceLocation,
    override val selectorKind: MatterSelectorKind,
    val suggestions: SuggestionProvider<CommandSourceStack>,
    val validate: (ResourceLocation) -> TargetValidation,
) : MatterCommandDef

sealed interface TargetValidation {
    data object Valid : TargetValidation
    data class Invalid(val message: Component) : TargetValidation
}
