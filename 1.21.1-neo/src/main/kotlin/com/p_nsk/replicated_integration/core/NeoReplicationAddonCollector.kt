package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.addon.RegisterNeoReplicationAddonsEvent
import net.neoforged.neoforge.common.NeoForge

@OptIn(ReplicationAddonLoadSafetyContract::class)
object NeoReplicationAddonCollector {
    private val collectedAddons: List<NeoReplicationAddon> by lazy {
        val event = RegisterNeoReplicationAddonsEvent()
        NeoForge.EVENT_BUS.post(event)
        event.addons().sortedBy { it.id }.also(::validateUniqueIds)
    }

    fun addons(): List<NeoReplicationAddon> =
        collectedAddons

    private fun validateUniqueIds(addons: List<NeoReplicationAddon>) {
        val duplicates = addons
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "Duplicate replication addon id(s): ${duplicates.sorted().joinToString(", ")}"
        }
    }
}
