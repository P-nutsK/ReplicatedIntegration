package com.p_nsk.replicated_integration.command

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.p_nsk.replicated_integration.api.command.MatterCommandSupport
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object MatterCommandArguments {
    fun withOptionalMatterType(
        builder: RequiredArgumentBuilder<CommandSourceStack, *>,
        execute: (CommandContext<CommandSourceStack>) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        builder.also { withMatterTypes ->
            MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                withMatterTypes.then(
                    Commands.literal(name)
                        .executes { context -> execute(context) }
                )
            }
        }

    fun withSetMatterValue(
        builder: RequiredArgumentBuilder<CommandSourceStack, *>,
        execute: (CommandContext<CommandSourceStack>) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        builder
            .then(
                Commands.literal("all")
                    .then(
                        allMatterArguments(0) { context ->
                            execute(context)
                        }
                    )
            )
            .also { withMatterTypes ->
                MatterCommandSupport.allMatterTypes.forEach { (name, _) ->
                    withMatterTypes.then(
                        Commands.literal(name)
                            .then(
                                Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                    .executes { context -> execute(context) }
                            )
                    )
                }
            }

    private fun allMatterArguments(
        index: Int,
        execute: (CommandContext<CommandSourceStack>) -> Int,
    ): RequiredArgumentBuilder<CommandSourceStack, Double> {
        val (name, _) = MatterCommandSupport.allMatterTypes[index]
        val argument = Commands.argument(name, DoubleArgumentType.doubleArg(0.0))

        return if (index == MatterCommandSupport.allMatterTypes.lastIndex) {
            argument.executes { context -> execute(context) }
        } else {
            argument.then(allMatterArguments(index + 1, execute))
        }
    }
}
