package com.p_nsk.replicated_integration.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent

object MatterCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        MatterCommandTree.register(dispatcher)
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }
}
