package com.p_nsk.replicated_integration.core

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterNodeCommands
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.ResourceLocation

@MatterNodeDsl
class NeoMatterNodeBuilder(
    private val nodeType: LiteResourceLocation,
) {
    private var valueCommand: NeoMatterCommand? = null
    private var tagCommand: NeoMatterCommand? = null

    fun value(
        literal: String,
        suggestions: SuggestionProvider<CommandSourceStack>,
        validate: (ResourceLocation) -> TargetValidation,
    ) {
        check(valueCommand == null) {
            "Value command is already registered for $nodeType"
        }

        valueCommand = NeoMatterCommand(
            literal = literal,
            nodeType = nodeType,
            selectorKind = MatterSelectorKind.NODE,
            suggestions = suggestions,
            validate = validate,
        )
    }

    fun tag(
        literal: String,
        suggestions: SuggestionProvider<CommandSourceStack>,
        validate: (ResourceLocation) -> TargetValidation,
    ) {
        check(tagCommand == null) {
            "Tag command is already registered for $nodeType"
        }

        tagCommand = NeoMatterCommand(
            literal = literal,
            nodeType = nodeType,
            selectorKind = MatterSelectorKind.TAG,
            suggestions = suggestions,
            validate = validate,
        )
    }

    fun buildCommands(): MatterNodeCommands<NeoMatterCommand> =
        MatterNodeCommands(
            value = valueCommand,
            tag = tagCommand,
        )
}
