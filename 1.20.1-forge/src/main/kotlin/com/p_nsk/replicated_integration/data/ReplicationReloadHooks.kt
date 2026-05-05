package com.p_nsk.replicated_integration.data

import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object ReplicationReloadHooks {
    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(MatterNodeValueReloadListener)
    }
}
