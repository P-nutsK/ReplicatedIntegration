package com.p_nsk.replicated_integration.command

import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.api.command.MatterCommandSupport
import com.p_nsk.replicated_integration.api.command.nodeKey
import com.p_nsk.replicated_integration.api.command.selectorKey
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import com.p_nsk.replicated_integration.core.ForgeMatterCommand
import com.p_nsk.replicated_integration.core.TargetValidation
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ResourceLocationArgument

object MatterCommandParsing {
    private val matterTypeNames: Set<String>
        get() = MatterCommandSupport.allMatterTypes.mapTo(linkedSetOf()) { it.first }

    fun parseId(
        context: CommandContext<CommandSourceStack>,
        name: String,
    ): LiteResourceLocation {
        val parsed = ResourceLocationArgument.getId(context, name)
        return LiteResourceLocation.of(parsed.namespace, parsed.path)
    }

    fun targetNodeKey(
        context: CommandContext<CommandSourceStack>,
        target: ForgeMatterCommand,
    ): NodeKey =
        target.nodeKey(parseId(context, "id"))

    fun targetSelectorKey(
        context: CommandContext<CommandSourceStack>,
        target: ForgeMatterCommand,
    ): MatterSelectorKey =
        target.selectorKey(parseId(context, "id"))

    fun debugTagSelector(context: CommandContext<CommandSourceStack>): MatterSelectorKey =
        MatterSelectorKey(
            kind = MatterSelectorKind.TAG,
            type = parseId(context, "nodeType"),
            id = parseId(context, "id"),
        )

    fun requestedMatterType(context: CommandContext<CommandSourceStack>): String? =
        context.nodes
            .lastOrNull { it.node.name in matterTypeNames }
            ?.node
            ?.name

    fun validateTargetOrFail(
        context: CommandContext<CommandSourceStack>,
        target: ForgeMatterCommand,
    ): Boolean {
        val id = ResourceLocationArgument.getId(context, "id")
        return when (val result = target.validate(id)) {
            TargetValidation.Valid -> true
            is TargetValidation.Invalid -> {
                context.source.sendFailure(result.message)
                false
            }
        }
    }
}
