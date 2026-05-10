package com.p_nsk.replicated_integration.core

import com.mojang.brigadier.suggestion.SuggestionProvider
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterNodeCommands
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.ResourceLocation

@MatterNodeDsl
class ForgeMatterNodeBuilder(
    private val nodeType: LiteResourceLocation,
) {
    private var valueCommand: ForgeMatterCommand? = null
    private var tagCommand: ForgeMatterCommand? = null

    fun value(
        literal: String,
        suggestions: SuggestionProvider<CommandSourceStack>,
        validate: (ResourceLocation) -> TargetValidation,
    ) {
        check(valueCommand == null) {
            "Value command is already registered for $nodeType"
        }

        valueCommand = ForgeMatterCommand(
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

        tagCommand = ForgeMatterCommand(
            literal = literal,
            nodeType = nodeType,
            selectorKind = MatterSelectorKind.TAG,
            suggestions = suggestions,
            validate = validate,
        )
    }

    fun buildCommands(): MatterNodeCommands<ForgeMatterCommand> =
        MatterNodeCommands(
            value = valueCommand,
            tag = tagCommand,
        )
}

