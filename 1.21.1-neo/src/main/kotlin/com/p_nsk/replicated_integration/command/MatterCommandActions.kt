package com.p_nsk.replicated_integration.command

import com.buuz135.replication.calculation.ReplicationCalculation
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.api.command.MatterCommandMutation
import com.p_nsk.replicated_integration.api.command.MatterCommandSupport
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.formatNode
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import com.p_nsk.replicated_integration.api.selector.formatSelector
import com.p_nsk.replicated_integration.core.NeoReplicationCalculationService
import com.p_nsk.replicated_integration.data.NeoMatterConfigOverrides
import com.p_nsk.replicated_integration.data.NeoMatterRuntimeOverrides
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component

object MatterCommandActions {
    fun getNode(
        context: CommandContext<CommandSourceStack>,
        node: NodeKey,
    ): Int {
        val explicitValue = MatterNodeDebugCache.explicit(node)
        val solvedValue = MatterNodeDebugCache.get(node)
        val label = NeoReplicationCalculationService.nodeTypes().formatNode(node)
        val requestedMatterType = MatterCommandParsing.requestedMatterType(context)

        when (explicitValue) {
            is ExplicitMatterValue.Deny -> {
                context.source.sendSuccess({
                    Component.literal("Matter for ")
                        .append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(": denied").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" via ${explicitValue.source.displayName}").withStyle(ChatFormatting.GRAY))
                }, false)
            }

            else -> {
                val sourceLabel = MatterCommandSupport.sourceLabel(explicitValue, solvedValue)
                context.source.sendSuccess({
                    Component.literal("Matter for ")
                        .append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" [$sourceLabel]").withStyle(ChatFormatting.GRAY))
                }, false)

                val compound = (explicitValue as? ExplicitMatterValue.Set)?.compound ?: solvedValue
                if (compound == null) {
                    context.source.sendFailure(Component.literal("No matter value is available for $label"))
                    return 0
                }

                val entries = filterMatterEntries(compound, requestedMatterType)
                if (requestedMatterType != null && entries.isEmpty()) {
                    context.source.sendFailure(Component.literal("$label has no $requestedMatterType matter"))
                    return 0
                }

                emitMatterEntries(context.source, entries)
            }
        }

        return 1
    }

    fun getSelector(
        context: CommandContext<CommandSourceStack>,
        selector: MatterSelectorKey,
    ): Int {
        val explicitValue = MatterNodeDebugCache.selector(selector)
        val label = NeoReplicationCalculationService.nodeTypes().formatSelector(selector)
        val requestedMatterType = MatterCommandParsing.requestedMatterType(context)

        when (explicitValue) {
            is ExplicitMatterValue.Deny -> {
                context.source.sendSuccess({
                    Component.literal("Selector ")
                        .append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(": denied").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" via ${explicitValue.source.displayName}").withStyle(ChatFormatting.GRAY))
                }, false)
                return 1
            }

            is ExplicitMatterValue.Set -> {
                context.source.sendSuccess({
                    Component.literal("Selector ")
                        .append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" [${explicitValue.source.displayName}]").withStyle(ChatFormatting.GRAY))
                }, false)

                val entries = filterMatterEntries(explicitValue.compound, requestedMatterType)
                if (requestedMatterType != null && entries.isEmpty()) {
                    context.source.sendFailure(Component.literal("$label has no $requestedMatterType matter"))
                    return 0
                }

                emitMatterEntries(context.source, entries)
                return 1
            }

            null -> {
                context.source.sendFailure(Component.literal("No explicit selector value is available for $label"))
                return 0
            }
        }
    }

    fun setSelector(
        context: CommandContext<CommandSourceStack>,
        selector: MatterSelectorKey,
    ): Int {
        val server = context.source.server

        val compound =
            if (context.nodes.any { it.node.name == "all" }) {
                buildAllCompound(context) ?: run {
                    context.source.sendFailure(Component.literal("At least one all-value must be positive"))
                    return 0
                }
            } else {
                val type = MatterCommandParsing.requestedMatterType(context) ?: return 0
                val amount = DoubleArgumentType.getDouble(context, "amount")
                MatterCommandMutation.setSingleMatter(
                    current = currentSelectorCompound(server, selector),
                    matterType = MatterCommandSupport.singleMatterType(type)!!,
                    amount = amount,
                )
            }

        NeoMatterRuntimeOverrides.set(server, selector, compound)
        ReplicationCalculation.calculateRecipes(server.registryAccess())

        context.source.sendSuccess({
            Component.literal(
                "Updated ${NeoReplicationCalculationService.nodeTypes().formatSelector(selector)} and queued recalculation."
            )
        }, true)

        return 1
    }

    private fun currentSelectorCompound(
        server: net.minecraft.server.MinecraftServer,
        selector: MatterSelectorKey,
    ): LiteMatterCompound =
        (NeoMatterRuntimeOverrides.snapshot(server)[selector] as? ExplicitMatterValue.Set)?.compound
            ?: (NeoMatterConfigOverrides.snapshot()[selector] as? ExplicitMatterValue.Set)?.compound
            ?: LiteMatterCompound.EMPTY

    fun denySelector(
        context: CommandContext<CommandSourceStack>,
        selector: MatterSelectorKey,
    ): Int {
        NeoMatterRuntimeOverrides.deny(context.source.server, selector)
        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())

        context.source.sendSuccess({
            Component.literal(
                "Denied ${NeoReplicationCalculationService.nodeTypes().formatSelector(selector)} and queued recalculation."
            )
        }, true)

        return 1
    }

    fun resetSelector(
        context: CommandContext<CommandSourceStack>,
        selector: MatterSelectorKey,
    ): Int {
        NeoMatterRuntimeOverrides.reset(context.source.server, selector)
        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())

        context.source.sendSuccess({
            Component.literal(
                "Reset runtime override for ${NeoReplicationCalculationService.nodeTypes().formatSelector(selector)} and queued recalculation."
            )
        }, true)

        return 1
    }

    fun commitRuntimeOverrides(context: CommandContext<CommandSourceStack>): Int {
        val committed = NeoMatterConfigOverrides.commit(context.source.server)

        if (committed == 0) {
            context.source.sendFailure(Component.literal("No runtime overrides are present to commit."))
            return 0
        }

        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())

        context.source.sendSuccess({
            Component.literal("Committed $committed runtime override(s) to config and queued recalculation.")
        }, true)

        return 1
    }

    private fun buildAllCompound(context: CommandContext<CommandSourceStack>): LiteMatterCompound? {
        val values = linkedMapOf<LiteResourceLocation, Double>()

        for ((name, type) in MatterCommandSupport.allMatterTypes) {
            val amount = DoubleArgumentType.getDouble(context, name)
            if (amount > 0.0) {
                values[type] = amount
            }
        }

        return values.takeIf { it.isNotEmpty() }?.let(::LiteMatterCompound)
    }

    private fun filterMatterEntries(
        compound: LiteMatterCompound,
        requestedMatterType: String?,
    ) = compound.values.entries
        .sortedBy { it.key.toString() }
        .filter { entry ->
            requestedMatterType == null || MatterCommandSupport.singleMatterType(requestedMatterType) == entry.key
        }

    private fun emitMatterEntries(
        source: CommandSourceStack,
        entries: List<Map.Entry<LiteResourceLocation, Double>>,
    ) {
        for ((matterId, amount) in entries) {
            source.sendSuccess({
                Component.literal("- ")
                    .append(Component.literal(matterId.toString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": ${MatterCommandSupport.formatAmount(amount)}"))
            }, false)
        }
    }
}
