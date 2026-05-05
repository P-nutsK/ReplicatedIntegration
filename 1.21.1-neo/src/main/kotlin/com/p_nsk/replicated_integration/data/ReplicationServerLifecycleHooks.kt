package com.p_nsk.replicated_integration.data

import com.buuz135.replication.calculation.ReplicationCalculation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent

object ReplicationServerLifecycleHooks {
    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        ReplicationCalculation.calculateRecipes(event.server.registryAccess())
    }
}
