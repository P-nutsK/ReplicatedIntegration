package com.p_nsk.replicated_integration.command

import com.buuz135.replication.calculation.ReplicationCalculation
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterCommandSupport
import com.p_nsk.replicated_integration.api.MatterNodeFormatter
import com.p_nsk.replicated_integration.api.MatterNodeKey
import com.p_nsk.replicated_integration.api.MatterNodes
import com.p_nsk.replicated_integration.bridge.NeoReplicationCalculationService
import com.p_nsk.replicated_integration.data.NeoMatterRuntimeOverrides
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.bus.api.SubscribeEvent

object MatterCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("matter")
                .then(getBranch())
                .then(setBranch())
                .then(denyBranch())
                .then(resetBranch())
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

    private fun setBranch() =
        Commands.literal("set")
            .requires { it.hasPermission(2) }
            .then(itemSelector(::setSelector))
            .then(typeSelector(::setSelector))

    private fun denyBranch() =
        Commands.literal("deny")
            .requires { it.hasPermission(2) }
            .then(itemSelector(::denyNode))
            .then(typeSelector(::denyNode))

    private fun resetBranch() =
        Commands.literal("reset")
            .requires { it.hasPermission(2) }
            .then(itemSelector(::resetNode))
            .then(typeSelector(::resetNode))

    private fun itemSelector(action: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int) =
        Commands.literal("item")
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .executes { context -> action(context, MatterNodes.item(parseId(context, "id"))) }
                    .applySetArguments { node -> action(this, node) }
            )

    private fun itemGetSelector(action: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int) =
        Commands.literal("item")
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .executes { context -> action(context, MatterNodes.item(parseId(context, "id"))) }
                    .applyGetArguments { node -> action(this, node) }
            )

    private fun typeSelector(action: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int) =
        Commands.literal("type")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context -> action(context, MatterNodeKey(parseId(context, "nodeType"), parseId(context, "id"))) }
                            .applySetArguments { node -> action(this, node) }
                    )
            )

    private fun typeGetSelector(action: (CommandContext<CommandSourceStack>, MatterNodeKey) -> Int) =
        Commands.literal("type")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context -> action(context, MatterNodeKey(parseId(context, "nodeType"), parseId(context, "id"))) }
                            .applyGetArguments { node -> action(this, node) }
                    )
            )

    private fun com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, *>.applySetArguments(
        execute: CommandContext<CommandSourceStack>.(MatterNodeKey) -> Int,
    ) =
        then(
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
                                                                                .executes {
                                                                                    execute(
                                                                                        it,
                                                                                        selectorNode(it),
                                                                                    )
                                                                                }
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        ).also { builder ->
            MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                builder.then(
                    Commands.literal(name)
                        .then(
                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.000001))
                                .executes {
                                    execute(
                                        it,
                                        selectorNode(it),
                                    )
                                }
                        )
                )
            }
        }

    private fun com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, *>.applyGetArguments(
        execute: CommandContext<CommandSourceStack>.(MatterNodeKey) -> Int,
    ) =
        also { builder ->
            MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                builder.then(
                    Commands.literal(name)
                        .executes {
                            execute(
                                it,
                                selectorNode(it),
                            )
                        }
                )
            }
        }

    private fun selectorNode(context: CommandContext<CommandSourceStack>): MatterNodeKey =
        if (context.nodes.any { it.node.name == "nodeType" }) {
            MatterNodeKey(parseId(context, "nodeType"), parseId(context, "id"))
        } else {
            MatterNodes.item(parseId(context, "id"))
        }

    private fun getNode(context: CommandContext<CommandSourceStack>, node: MatterNodeKey): Int {
        val explicitValue = MatterNodeDebugCache.explicit(node)
        val solvedValue = MatterNodeDebugCache.get(node)
        val label = MatterNodeFormatter.formatNode(node, NeoReplicationCalculationService.nodeTypes())
        val requestedMatterType =
            context.nodes
                .lastOrNull { it.node.name in MatterCommandSupport.allMatterTypes.map { pair -> pair.first } }
                ?.node
                ?.name
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
                val entries =
                    compound.values.entries
                        .sortedBy { it.key.toString() }
                        .filter { entry ->
                            requestedMatterType == null || MatterCommandSupport.singleMatterType(requestedMatterType) == entry.key
                        }
                if (requestedMatterType != null && entries.isEmpty()) {
                    context.source.sendFailure(Component.literal("$label has no ${requestedMatterType} matter"))
                    return 0
                }
                for ((matterId, amount) in entries) {
                    context.source.sendSuccess({
                        Component.literal("- ")
                            .append(Component.literal(matterId.toString()).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(": ${MatterCommandSupport.formatAmount(amount)}"))
                    }, false)
                }
            }
        }
        return 1
    }

    private fun setSelector(context: CommandContext<CommandSourceStack>, node: MatterNodeKey): Int {
        val server = context.source.server
        val compound =
            if (context.nodes.any { it.node.name == "all" }) {
                buildAllCompound(context) ?: run {
                    context.source.sendFailure(Component.literal("At least one all-value must be positive"))
                    return 0
                }
            } else {
                val type = context.nodes.last { it.node.name in MatterCommandSupport.allMatterTypes.map { pair -> pair.first } }.node.name
                val amount = DoubleArgumentType.getDouble(context, "amount")
                val current = (NeoMatterRuntimeOverrides.snapshot(server)[node] as? ExplicitMatterValue.Set)?.compound?.values?.toMutableMap() ?: linkedMapOf()
                current[MatterCommandSupport.singleMatterType(type)!!] = amount
                LiteMatterCompound(current)
            }
        NeoMatterRuntimeOverrides.set(server, node, compound)
        ReplicationCalculation.calculateRecipes(server.registryAccess())
        context.source.sendSuccess({
            Component.literal("Updated ${MatterNodeFormatter.formatNode(node, NeoReplicationCalculationService.nodeTypes())} and queued recalculation.")
        }, true)
        return 1
    }

    private fun denyNode(context: CommandContext<CommandSourceStack>, node: MatterNodeKey): Int {
        NeoMatterRuntimeOverrides.deny(context.source.server, node)
        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())
        context.source.sendSuccess({
            Component.literal("Denied ${MatterNodeFormatter.formatNode(node, NeoReplicationCalculationService.nodeTypes())} and queued recalculation.")
        }, true)
        return 1
    }

    private fun resetNode(context: CommandContext<CommandSourceStack>, node: MatterNodeKey): Int {
        NeoMatterRuntimeOverrides.reset(context.source.server, node)
        ReplicationCalculation.calculateRecipes(context.source.server.registryAccess())
        context.source.sendSuccess({
            Component.literal("Reset runtime override for ${MatterNodeFormatter.formatNode(node, NeoReplicationCalculationService.nodeTypes())} and queued recalculation.")
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

    private fun parseId(context: CommandContext<CommandSourceStack>, name: String): LiteResourceLocation {
        val parsed = ResourceLocationArgument.getId(context, name)
        return LiteResourceLocation.of(parsed.namespace, parsed.path)
    }
}
