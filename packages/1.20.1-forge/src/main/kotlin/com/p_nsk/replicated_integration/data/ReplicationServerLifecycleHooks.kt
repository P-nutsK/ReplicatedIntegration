package com.p_nsk.replicated_integration.data

import com.buuz135.replication.calculation.ReplicationCalculation
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object ReplicationServerLifecycleHooks {
    @Suppress("unused")
    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        ReplicationCalculation.calculateRecipes()
    }
}
