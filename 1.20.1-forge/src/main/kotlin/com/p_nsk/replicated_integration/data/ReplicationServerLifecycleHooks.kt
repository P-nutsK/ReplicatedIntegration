package com.p_nsk.replicated_integration.data

import com.buuz135.replication.calculation.ReplicationCalculation
import com.p_nsk.replicated_integration.core.ForgeReplicationCalculationService
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object ReplicationServerLifecycleHooks {
    @Suppress("unused")
    @SubscribeEvent
    fun onServerStarted(event: ServerStartedEvent) {
        ReplicationCalculation.organizeRecipes(event.server.recipeManager, event.server.registryAccess())
        ReplicationCalculation.calculateRecipes()
    }

    @Suppress("unused")
    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return
        ForgeReplicationCalculationService.syncLatestToPlayer(player)
    }
}
