package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.addon.RegisterForgeReplicationAddonsEvent
import net.minecraftforge.common.MinecraftForge

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ForgeReplicationAddonCollector {
    private val collectedAddons: List<ForgeReplicationAddon> by lazy {
        val event = RegisterForgeReplicationAddonsEvent()
        MinecraftForge.EVENT_BUS.post(event)
        event.addons().sortedBy { it.id }.also(::validateUniqueIds)
    }

    fun addons(): List<ForgeReplicationAddon> =
        collectedAddons

    private fun validateUniqueIds(addons: List<ForgeReplicationAddon>) {
        val duplicates = addons
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "Duplicate replication addon id(s): ${duplicates.sorted().joinToString(", ")}"
        }
    }
}
