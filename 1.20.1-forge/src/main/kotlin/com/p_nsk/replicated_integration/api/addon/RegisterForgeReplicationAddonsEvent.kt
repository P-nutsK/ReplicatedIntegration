package com.p_nsk.replicated_integration.api.addon

import com.p_nsk.replicated_integration.core.ForgeReplicationAddon
import net.minecraftforge.eventbus.api.Event

/**
 * Register additional replication addons from a Forge mod constructor by adding a listener to
 * MinecraftForge.EVENT_BUS and calling [register] from that listener.
 */
@OptIn(ReplicationAddonLoadSafetyContract::class)
class RegisterForgeReplicationAddonsEvent : Event() {
    private val registeredAddons = mutableListOf<ForgeReplicationAddon>()

    fun register(addon: ForgeReplicationAddon) {
        registeredAddons += addon
    }

    internal fun addons(): List<ForgeReplicationAddon> =
        registeredAddons.toList()
}
