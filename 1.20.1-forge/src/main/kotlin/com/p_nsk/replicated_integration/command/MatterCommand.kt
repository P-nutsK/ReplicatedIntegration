package com.p_nsk.replicated_integration.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object MatterCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        MatterCommandTree.register(dispatcher)
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }
}
