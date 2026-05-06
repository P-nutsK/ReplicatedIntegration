package com.p_nsk.replicated_integration.command

import com.buuz135.replication.calculation.ReplicationCalculation
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.command.MatterCommandSupport
import com.p_nsk.replicated_integration.api.node.MatterNodeFormatter
import com.p_nsk.replicated_integration.api.node.MatterNodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.selector.MatterSelectorFormatter
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import com.p_nsk.replicated_integration.bridge.NeoReplicationCalculationService
import com.p_nsk.replicated_integration.data.NeoMatterConfigOverrides
import com.p_nsk.replicated_integration.data.NeoMatterRuntimeOverrides
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent

object MatterCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("repint")
                .then(
                    Commands.literal("matter")
                        .then(getBranch())
                        .then(setBranch())
                        .then(denyBranch())
                        .then(resetBranch())
                )
                .then(commitBranch())
        )
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }

    private fun getBranch() =
        Commands.literal("get")
            .then(itemGetSelector(::getNode))
            .then(typeGetSelector(::getNode))
            .then(tagGetSelector(::getTagSelector))

    private fun setBranch() =
        Commands.literal("set")
            .requires { it.hasPermission(2) }
            .then(itemSelector(::setSelector))
            .then(typeSelector(::setSelector))
            .then(tagSelector(::setSelector))

    private fun denyBranch() =
        Commands.literal("deny")
            .requires { it.hasPermission(2) }
            .then(itemSelector(::denySelector))
            .then(typeSelector(::denySelector))
            .then(tagSelector(::denySelector))

    private fun resetBranch() =
        Commands.literal("reset")
            .requires { it.hasPermission(2) }
            .then(itemSelector(::resetSelector))
            .then(typeSelector(::resetSelector))
            .then(tagSelector(::resetSelector))

    private fun commitBranch() =
        Commands.literal("commit")
            .requires { it.hasPermission(2) }
            .executes(::commitRuntimeOverrides)

    private fun itemSelector(action: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int) =
        Commands.literal("item")
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .executes { context -> action(context, MatterSelectorKey(MatterSelectorKind.NODE, MatterNodes.ITEM, parseId(context, "id"))) }
                    .let { applySetArguments(it, { context -> MatterSelectorKey(MatterSelectorKind.NODE, MatterNodes.ITEM, parseId(context, "id")) }, action) }
            )

    private fun typeSelector(action: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int) =
        Commands.literal("type")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context -> action(context, concreteSelector(context)) }
                            .let { applySetArguments(it, ::concreteSelector, action) }
                    )
            )

    private fun tagSelector(action: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int) =
        Commands.literal("tag")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context -> action(context, tagSelector(context)) }
                            .let { applySetArguments(it, ::tagSelector, action) }
                    )
            )

    private fun itemGetSelector(action: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int) =
        Commands.literal("item")
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .executes { context -> action(context, MatterNodes.item(parseId(context, "id"))) }
                    .let { applyGetArguments(it, { context -> MatterNodes.item(parseId(context, "id")) }, action) }
            )

    private fun typeGetSelector(action: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int) =
        Commands.literal("type")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context -> action(context, MatterNodeKey(parseId(context, "nodeType"), parseId(context, "id"))) }
                            .let { applyGetArguments(it, { context -> MatterNodeKey(parseId(context, "nodeType"), parseId(context, "id")) }, action) }
                    )
            )

    private fun tagGetSelector(action: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int) =
        Commands.literal("tag")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context -> action(context, tagSelector(context)) }
                            .let { applyTagGetArguments(it, ::tagSelector, action) }
                    )
            )

    private fun applySetArguments(
        builder: RequiredArgumentBuilder<CommandSourceStack, *>,
        selectorOf: (CommandContext<CommandSourceStack>) -> MatterSelectorKey,
        execute: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        builder.then(
            Commands.literal("all")
                .then(
                    Commands.argument("earth", DoubleArgumentType.doubleArg(0.0))
                        .then(
                            Commands.argument("nether", DoubleArgumentType.doubleArg(0.0))
                                .then(
                                    Commands.argument("organic", DoubleArgumentType.doubleArg(0.0))
                                        .then(
                                            Commands.argument("ender", DoubleArgumentType.doubleArg(0.0))
                                                .then(
                                                    Commands.argument("metallic", DoubleArgumentType.doubleArg(0.0))
                                                        .then(
                                                            Commands.argument("precious", DoubleArgumentType.doubleArg(0.0))
                                                                .then(
                                                                    Commands.argument("living", DoubleArgumentType.doubleArg(0.0))
                                                                        .then(
                                                                            Commands.argument("quantum", DoubleArgumentType.doubleArg(0.0))
                                                                                .executes { execute(it, selectorOf(it)) }
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).also { withMatterTypes ->
            MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                withMatterTypes.then(
                    Commands.literal(name)
                        .then(
                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.000001))
                                .executes { execute(it, selectorOf(it)) }
                        )
                )
            }
        }

    private fun applyGetArguments(
        builder: RequiredArgumentBuilder<CommandSourceStack, *>,
        nodeOf: (CommandContext<CommandSourceStack>) -> MatterNodeKey,
        execute: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        builder.also { withMatterTypes ->
            MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                withMatterTypes.then(
                    Commands.literal(name)
                        .executes { execute(it, nodeOf(it)) }
                )
            }
        }

    private fun applyTagGetArguments(
        builder: RequiredArgumentBuilder<CommandSourceStack, *>,
        selectorOf: (CommandContext<CommandSourceStack>) -> MatterSelectorKey,
        execute: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        builder.also { withMatterTypes ->
            MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                withMatterTypes.then(
                    Commands.literal(name)
                        .executes { execute(it, selectorOf(it)) }
                )
            }
        }

    private fun getNode(context: CommandContext<CommandSourceStack>, node: MatterNodeKey): Int {
        val explicitValue = MatterNodeDebugCache.explicit(node)
        val solvedValue = MatterNodeDebugCache.get(node)
        val label = MatterNodeFormatter.formatNode(node, NeoReplicationCalculationService.nodeTypes())
        val requestedMatterType = requestedMatterType(context)
        when (explicitValue) {
            is ExplicitMatterValue.Deny -> {
                context.source.sendSuccess({
                    Component.literal("Matter for ").append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(": denied").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" via ${explicitValue.source.displayName}").withStyle(ChatFormatting.GRAY))
                }, false)
            }
            else -> {
                val sourceLabel = MatterCommandSupport.sourceLabel(explicitValue, solvedValue)
                context.source.sendSuccess({
                    Component.literal("Matter for ").append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" [$sourceLabel]").withStyle(ChatFormatting.GRAY))
                }, false)
                val compound = (explicitValue as? ExplicitMatterValue.Set)?.compound ?: solvedValue
                if (compound == null) {
                    context.source.sendFailure(Component.literal("No matter value is available for $label"))
                    return 0
                }
                val entries = filterMatterEntries(compound, requestedMatterType)
                if (requestedMatterType != null && entries.isEmpty()) {
                    context.source.sendFailure(Component.literal("$label has no ${requestedMatterType} matter"))
                    return 0
                }
                emitMatterEntries(context.source, entries)
            }
        }
        return 1
    }

    private fun getTagSelector(context: CommandContext<CommandSourceStack>, selector: MatterSelectorKey): Int {
        val explicitValue = MatterNodeDebugCache.selector(selector)
        val label = MatterSelectorFormatter.format(selector, NeoReplicationCalculationService.nodeTypes())
        val requestedMatterType = requestedMatterType(context)
        when (explicitValue) {
            is ExplicitMatterValue.Deny -> {
                context.source.sendSuccess({
                    Component.literal("Selector ").append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(": denied").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" via ${explicitValue.source.displayName}").withStyle(ChatFormatting.GRAY))
                }, false)
                return 1
            }
            is ExplicitMatterValue.Set -> {
                context.source.sendSuccess({
                    Component.literal("Selector ").append(Component.literal(label).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" [${explicitValue.source.displayName}]").withStyle(ChatFormatting.GRAY))
                }, false)
                val entries = filterMatterEntries(explicitValue.compound, requestedMatterType)
                if (requestedMatterType != null && entries.isEmpty()) {
                    context.source.sendFailure(Component.literal("$label has no ${requestedMatterType} matter"))
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

    private fun setSelector(context: CommandContext<CommandSourceStack>, selector: MatterSelectorKey): Int {
        val server = context.source.server
        val compound =
            if (context.nodes.any { it.node.name == "all" }) {
                buildAllCompound(context) ?: run {
                    context.source.sendFailure(Component.literal("At least one all-value must be positive"))
                    return 0
                }
            } else {
                val type = requestedMatterType(context) ?: return 0
                val amount = DoubleArgumentType.getDouble(context, "amount")
                val current = (NeoMatterRuntimeOverrides.snapshot(server)[selector] as? ExplicitMatterValue.Set)?.compound?.values?.toMutableMap() ?: linkedMapOf()
                current[MatterCommandSupport.singleMatterType(type)!!] = amount
                LiteMatterCompound(current)
            }
        NeoMatterRuntimeOverrides.set(server, selector, compound)
        ReplicationCalculation.calculateRecipes(server.registryAccess())
        context.source.sendSuccess({
            Component.literal("Updated ${MatterSelectorFormatter.format(selector, NeoReplicationCalculationService.nodeTypes())} and queued recalculation.")
        }, true)
        return 1
    }

    private fun denySelector(context: CommandContext<CommandSourceStack>, selector: MatterSelectorKey): Int {
        NeoMatterRuntimeOverrides.deny(context.source.server, selector)
        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())
        context.source.sendSuccess({
            Component.literal("Denied ${MatterSelectorFormatter.format(selector, NeoReplicationCalculationService.nodeTypes())} and queued recalculation.")
        }, true)
        return 1
    }

    private fun resetSelector(context: CommandContext<CommandSourceStack>, selector: MatterSelectorKey): Int {
        NeoMatterRuntimeOverrides.reset(context.source.server, selector)
        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())
        context.source.sendSuccess({
            Component.literal("Reset runtime override for ${MatterSelectorFormatter.format(selector, NeoReplicationCalculationService.nodeTypes())} and queued recalculation.")
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

    private fun concreteSelector(context: CommandContext<CommandSourceStack>) =
        MatterSelectorKey(MatterSelectorKind.NODE, parseId(context, "nodeType"), parseId(context, "id"))

    private fun tagSelector(context: CommandContext<CommandSourceStack>) =
        MatterSelectorKey(MatterSelectorKind.TAG, parseId(context, "nodeType"), parseId(context, "id"))

    private fun parseId(context: CommandContext<CommandSourceStack>, name: String): LiteResourceLocation {
        val parsed = ResourceLocationArgument.getId(context, name)
        return LiteResourceLocation.of(parsed.namespace, parsed.path)
    }

    private fun requestedMatterType(context: CommandContext<CommandSourceStack>): String? =
        context.nodes
            .lastOrNull { it.node.name in MatterCommandSupport.allMatterTypes.map { pair -> pair.first } }
            ?.node
            ?.name

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

    private fun commitRuntimeOverrides(context: CommandContext<CommandSourceStack>): Int {
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
}
