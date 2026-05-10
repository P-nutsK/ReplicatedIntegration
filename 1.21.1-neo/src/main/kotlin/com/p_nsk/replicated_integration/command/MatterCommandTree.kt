package com.p_nsk.replicated_integration.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import com.p_nsk.replicated_integration.core.NeoMatterCommand
import com.p_nsk.replicated_integration.core.NeoReplicationCalculationService
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument

object MatterCommandTree {
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

    private fun commandTargets(): List<NeoMatterCommand> =
        NeoReplicationCalculationService.commandTargets()

    private fun getBranch() =
        Commands.literal("get").also { branch ->
            commandTargets().forEach { target ->
                branch.then(targetGetSelector(target))
            }

            branch.then(debugTypeGetSelector())
            branch.then(debugTagGetSelector())
        }

    private fun setBranch() =
        Commands.literal("set")
            .requires { it.hasPermission(2) }
            .also { branch ->
                commandTargets().forEach { target ->
                    branch.then(targetSetSelector(target))
                }
            }

    private fun denyBranch() =
        Commands.literal("deny")
            .requires { it.hasPermission(2) }
            .also { branch ->
                commandTargets().forEach { target ->
                    branch.then(
                        targetSelector(
                            target = target,
                            action = MatterCommandActions::denySelector,
                        )
                    )
                }
            }

    private fun resetBranch() =
        Commands.literal("reset")
            .requires { it.hasPermission(2) }
            .also { branch ->
                commandTargets().forEach { target ->
                    branch.then(
                        targetSelector(
                            target = target,
                            action = MatterCommandActions::resetSelector,
                        )
                    )
                }
            }

    private fun commitBranch() =
        Commands.literal("commit")
            .requires { it.hasPermission(2) }
            .executes(MatterCommandActions::commitRuntimeOverrides)

    private fun targetGetSelector(target: NeoMatterCommand): ArgumentBuilder<CommandSourceStack, *> =
        Commands.literal(target.literal)
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(target.suggestions)
                    .executes { context ->
                        if (!MatterCommandParsing.validateTargetOrFail(context, target)) return@executes 0
                        executeGetTarget(context, target)
                    }
                    .let { builder ->
                        MatterCommandArguments.withOptionalMatterType(builder) { context ->
                            if (!MatterCommandParsing.validateTargetOrFail(context, target)) return@withOptionalMatterType 0
                            executeGetTarget(context, target)
                        }
                    }
            )

    private fun executeGetTarget(
        context: CommandContext<CommandSourceStack>,
        target: NeoMatterCommand,
    ): Int =
        when (target.selectorKind) {
            MatterSelectorKind.NODE ->
                MatterCommandActions.getNode(
                    context,
                    MatterCommandParsing.targetNodeKey(context, target),
                )

            MatterSelectorKind.TAG ->
                MatterCommandActions.getSelector(
                    context,
                    MatterCommandParsing.targetSelectorKey(context, target),
                )
        }

    private fun targetSetSelector(target: NeoMatterCommand): ArgumentBuilder<CommandSourceStack, *> =
        Commands.literal(target.literal)
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(target.suggestions)
                    .let { builder ->
                        MatterCommandArguments.withSetMatterValue(builder) { context ->
                            if (!MatterCommandParsing.validateTargetOrFail(context, target)) return@withSetMatterValue 0
                            MatterCommandActions.setSelector(
                                context,
                                MatterCommandParsing.targetSelectorKey(context, target),
                            )
                        }
                    }
            )

    private fun targetSelector(
        target: NeoMatterCommand,
        action: (CommandContext<CommandSourceStack>, MatterSelectorKey) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        Commands.literal(target.literal)
            .then(
                Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(target.suggestions)
                    .executes { context ->
                        if (!MatterCommandParsing.validateTargetOrFail(context, target)) return@executes 0
                        action(context, MatterCommandParsing.targetSelectorKey(context, target))
                    }
            )

    private fun debugTypeGetSelector(): ArgumentBuilder<CommandSourceStack, *> =
        Commands.literal("type")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context ->
                                MatterCommandActions.getNode(
                                    context,
                                    NodeKey(
                                        MatterCommandParsing.parseId(context, "nodeType"),
                                        MatterCommandParsing.parseId(context, "id"),
                                    )
                                )
                            }
                            .let { builder ->
                                MatterCommandArguments.withOptionalMatterType(builder) { context ->
                                    MatterCommandActions.getNode(
                                        context,
                                        NodeKey(
                                            MatterCommandParsing.parseId(context, "nodeType"),
                                            MatterCommandParsing.parseId(context, "id"),
                                        )
                                    )
                                }
                            }
                    )
            )

    private fun debugTagGetSelector(): ArgumentBuilder<CommandSourceStack, *> =
        Commands.literal("tag")
            .then(
                Commands.argument("nodeType", ResourceLocationArgument.id())
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .executes { context ->
                                MatterCommandActions.getSelector(
                                    context,
                                    MatterCommandParsing.debugTagSelector(context),
                                )
                            }
                            .let { builder ->
                                MatterCommandArguments.withOptionalMatterType(builder) { context ->
                                    MatterCommandActions.getSelector(
                                        context,
                                        MatterCommandParsing.debugTagSelector(context),
                                    )
                                }
                            }
                    )
            )
}
