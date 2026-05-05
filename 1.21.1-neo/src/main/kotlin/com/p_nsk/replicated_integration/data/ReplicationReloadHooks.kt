package com.p_nsk.replicated_integration.data

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.AddReloadListenerEvent

object ReplicationReloadHooks {
    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(MatterNodeValueReloadListener)
    }
}
