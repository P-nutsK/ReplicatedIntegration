package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.adapter.advanced_ae.ReplicationAdvancedAEAddon
import com.p_nsk.replicated_integration.adapter.ae2.ReplicationAE2Addon
import com.p_nsk.replicated_integration.adapter.draconic_evolution.ReplicationDraconicAddon
import com.p_nsk.replicated_integration.adapter.mekanism.ReplicationMekanismAddon
import com.p_nsk.replicated_integration.adapter.vanilla.ReplicationVanillaAddon
import com.p_nsk.replicated_integration.api.addon.RegisterNeoReplicationAddonsEvent
import net.neoforged.bus.api.SubscribeEvent

object NeoBuiltinReplicationAddons {
    @SubscribeEvent
    fun onRegisterReplicationAddons(event: RegisterNeoReplicationAddonsEvent) {
        event.register(ReplicationVanillaAddon)
        event.register(ReplicationAE2Addon)
        event.register(ReplicationAdvancedAEAddon)
        event.register(ReplicationDraconicAddon)
        event.register(ReplicationMekanismAddon)
    }
}
