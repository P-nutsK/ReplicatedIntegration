package com.p_nsk.replicated_integration.data

import com.buuz135.replication.calculation.ReplicationCalculation
import com.p_nsk.replicated_integration.Constants
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.TagsUpdatedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.server.ServerLifecycleHooks

object ReplicationReloadHooks {
    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(MatterNodeValueReloadListener)
        event.addListener(
            ResourceManagerReloadListener {
                ReplicationCalculation.organizeRecipes(
                    event.serverResources.recipeManager,
                    event.registryAccess,
                )
                Constants.LOGGER.info(
                    "Organized {} Replication default matter recipes during datapack reload",
                    ReplicationCalculation.DEFAULT_MATTER_RECIPE.size,
                )
            },
        )
    }

    @SubscribeEvent
    fun onTagsUpdated(event: TagsUpdatedEvent) {
        if (event.updateCause == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            val server = ServerLifecycleHooks.getCurrentServer() ?: return
            ReplicationCalculation.organizeRecipes(server.recipeManager, server.registryAccess())
            ReplicationCalculation.calculateRecipes()
        }
    }
}
