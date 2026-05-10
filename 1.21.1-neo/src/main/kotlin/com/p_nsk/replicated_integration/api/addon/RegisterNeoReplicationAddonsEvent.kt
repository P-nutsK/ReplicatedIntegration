package com.p_nsk.replicated_integration.api.addon

import com.p_nsk.replicated_integration.core.NeoReplicationAddon
import net.neoforged.bus.api.Event

/**
 * Register additional replication addons from a NeoForge mod constructor by adding a listener to
 * NeoForge.EVENT_BUS and calling [register] from that listener.
 */
@OptIn(ReplicationAddonLoadSafetyContract::class)
class RegisterNeoReplicationAddonsEvent : Event() {
    private val registeredAddons = mutableListOf<NeoReplicationAddon>()

    fun register(addon: NeoReplicationAddon) {
        registeredAddons += addon
    }

    internal fun addons(): List<NeoReplicationAddon> =
        registeredAddons.toList()
}
