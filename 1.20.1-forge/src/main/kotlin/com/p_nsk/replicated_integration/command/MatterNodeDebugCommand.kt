package com.p_nsk.replicated_integration.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.bridge.ForgeReplicationCalculationService
import com.p_nsk.replicated_integration.api.graph.ConversionGraph
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.MatterNodeFormatter
import com.p_nsk.replicated_integration.api.node.MatterNodeKey
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object MatterNodeDebugCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("repint")
                .then(
                    Commands.literal("matter")
                        .then(
                            Commands.literal("node")
                                .then(
                                    Commands.argument("type", ResourceLocationArgument.id())
                                        .then(
                                            Commands.argument("id", ResourceLocationArgument.id())
                                                .executes(::describeExplicitNode)
                                                .then(
                                                    Commands.literal("trace")
                                                        .executes { context -> traceExplicitNode(context, 1) }
                                                        .then(
                                                            Commands.argument("depth", IntegerArgumentType.integer(1, 4))
                                                                .executes { context ->
                                                                    traceExplicitNode(
                                                                        context,
                                                                        IntegerArgumentType.getInteger(context, "depth"),
                                                                    )
                                                                }
                                                        )
                                                )
                                        )
                                )
                        )
                )
        )
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }

    private fun describeExplicitNode(context: CommandContext<CommandSourceStack>): Int {
        val type = parseId(context, "type") ?: return 0
        val id = parseId(context, "id") ?: return 0
        return describeNode(context.source, MatterNodeKey(type, id))
    }

    private fun traceExplicitNode(context: CommandContext<CommandSourceStack>, depth: Int): Int {
        val type = parseId(context, "type") ?: return 0
        val id = parseId(context, "id") ?: return 0
        return traceNode(context.source, MatterNodeKey(type, id), depth)
    }

    @Suppress("RedundantNullableReturnType")
    private fun parseId(context: CommandContext<CommandSourceStack>, name: String): LiteResourceLocation? {
        val parsed = ResourceLocationArgument.getId(context, name)
        return LiteResourceLocation.of(parsed.namespace, parsed.path)
    }

    private fun describeNode(source: CommandSourceStack, node: MatterNodeKey): Int {
        if (MatterNodeDebugCache.isEmpty()) {
            source.sendFailure(
                Component.literal("No matter node data is cached yet. Trigger a data reload or join a loaded world first.")
            )
            return 0
        }

        val compound = MatterNodeDebugCache.get(node)
        if (compound == null || compound.values.isEmpty()) {
            source.sendSuccess(
                {
                    Component.literal(
                        "No matter value is cached for ${MatterNodeFormatter.formatNode(node, ForgeReplicationCalculationService.nodeTypes())}"
                    ).withStyle(ChatFormatting.YELLOW)
                },
                false,
            )
            return 1
        }

        source.sendSuccess(
            { buildSummary(node, compound) },
            false,
        )
        compound.values.entries
            .sortedBy { it.key.toString() }
            .forEach { (matterId, amount) ->
                source.sendSuccess(
                    {
                        Component.literal("- ")
                            .append(Component.literal(matterId.toString()).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(": ${formatAmount(amount)}").withStyle(ChatFormatting.WHITE))
                    },
                    false,
                )
        }
        return 1
    }

    private fun traceNode(source: CommandSourceStack, node: MatterNodeKey, depth: Int): Int {
        if (MatterNodeDebugCache.isEmpty()) {
            source.sendFailure(Component.literal("No matter node data is cached yet. Trigger a data reload or join a loaded world first."))
            return 0
        }

        val solved = MatterNodeDebugCache.solved()
        val graph = MatterNodeDebugCache.graph()
        val seen = linkedSetOf<MatterNodeKey>()
        source.sendSuccess(
            {
                Component.literal("Trace for ")
                    .append(Component.literal(MatterNodeFormatter.formatNode(node, ForgeReplicationCalculationService.nodeTypes())).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" depth=$depth").withStyle(ChatFormatting.GRAY))
            },
            false,
        )
        emitTrace(source, node, depth, 0, graph, solved, seen)
        return 1
    }

    private fun emitTrace(
        source: CommandSourceStack,
        node: MatterNodeKey,
        depth: Int,
        indent: Int,
        graph: ConversionGraph,
        solved: Map<MatterNodeKey, LiteMatterCompound>,
        seen: MutableSet<MatterNodeKey>,
    ) {
        val prefix = " ".repeat(indent * 2)
        val label = MatterNodeFormatter.formatNode(node, ForgeReplicationCalculationService.nodeTypes())
        val solvedValue = solved[node]
        val explicitValue = MatterNodeDebugCache.explicit(node)
        val conversions = graph.byOutputsNode[node].orEmpty()

        source.sendSuccess(
            {
                Component.literal(prefix)
                    .append(Component.literal(label).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" solved=").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(solvedValue?.let(::formatCompound) ?: "<none>").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" explicit=").withStyle(ChatFormatting.GRAY))
                    .append(
                        Component.literal(
                            when (explicitValue) {
                                is ExplicitMatterValue.Deny -> "<denied>"
                                is ExplicitMatterValue.Set -> formatCompound(explicitValue.compound)
                                null -> "<none>"
                            }
                        ).withStyle(ChatFormatting.GOLD)
                    )
                    .append(Component.literal(" incoming=${conversions.size}").withStyle(ChatFormatting.DARK_GRAY))
            },
            false,
        )

        if (depth <= 0 || !seen.add(node)) {
            return
        }

        conversions
            .sortedBy { it.id.toString() }
            .take(8)
            .forEach { conversion ->
                emitConversion(source, conversion, indent + 1, solved)
                if (depth > 1) {
                    conversion.consumes.forEach { consumed ->
                        emitTrace(source, consumed.node, depth - 1, indent + 2, graph, solved, seen)
                    }
                }
            }
    }

    private fun emitConversion(
        source: CommandSourceStack,
        conversion: MatterConversion,
        indent: Int,
        solved: Map<MatterNodeKey, LiteMatterCompound>,
    ) {
        val prefix = " ".repeat(indent * 2)
        val candidate = evaluateCandidate(conversion, solved)
        source.sendSuccess(
            {
                Component.literal(prefix)
                    .append(Component.literal("via ${conversion.id}").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" => ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(candidate?.let(::formatCompound) ?: "<missing inputs>").withStyle(ChatFormatting.WHITE))
            },
            false,
        )
        conversion.consumes.forEach { consume ->
            val known = solved[consume.node]
            source.sendSuccess(
                {
                    Component.literal(prefix)
                        .append(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal("${consume.amount} x ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(MatterNodeFormatter.formatNode(consume.node, ForgeReplicationCalculationService.nodeTypes())).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" = ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(known?.let(::formatCompound) ?: "<unknown>").withStyle(ChatFormatting.WHITE))
                },
                false,
            )
        }
        conversion.credits.forEach { credit ->
            val known = solved[credit.node]
            source.sendSuccess(
                {
                    Component.literal(prefix)
                        .append(Component.literal("- credit ").withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal("${credit.amount} x ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(MatterNodeFormatter.formatNode(credit.node, ForgeReplicationCalculationService.nodeTypes())).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" = ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(known?.let(::formatCompound) ?: "<unknown>").withStyle(ChatFormatting.WHITE))
                },
                false,
            )
        }
    }

    private fun evaluateCandidate(
        conversion: MatterConversion,
        solved: Map<MatterNodeKey, LiteMatterCompound>,
    ): LiteMatterCompound? {
        var result = LiteMatterCompound.EMPTY
        for (consume in conversion.consumes) {
            val value = solved[consume.node] ?: return null
            result = result.add(value.multiply(consume.amount.toDouble()))
        }
        for (credit in conversion.credits) {
            val value = solved[credit.node] ?: continue
            result = result.subtract(value.multiply(credit.amount.toDouble()))
        }
        return result.divide(conversion.produces.amount.toDouble())
    }

    private fun buildSummary(node: MatterNodeKey, compound: LiteMatterCompound): MutableComponent =
        Component.literal("Matter for ")
            .append(
                Component.literal(MatterNodeFormatter.formatNode(node, ForgeReplicationCalculationService.nodeTypes()))
                    .withStyle(ChatFormatting.GREEN)
            )
            .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("total=${formatAmount(compound.weight())}").withStyle(ChatFormatting.GOLD))

    private fun formatAmount(value: Double): String =
        if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.6f".format(value).trimEnd('0').trimEnd('.')
        }

    private fun formatCompound(compound: LiteMatterCompound): String =
        compound.values.entries
            .sortedBy { it.key.toString() }
            .joinToString(", ") { (matterId, amount) ->
                "${matterId}=${formatAmount(amount)}"
            }
}
