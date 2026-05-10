package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.adapter.advanced_ae.ReplicationAdvancedAEAddon
import com.p_nsk.replicated_integration.adapter.ae2.ReplicationAE2Addon
import com.p_nsk.replicated_integration.adapter.draconic_evolution.ReplicationDraconicAddon
import com.p_nsk.replicated_integration.adapter.mekanism.ReplicationMekanismAddon
import com.p_nsk.replicated_integration.adapter.vanilla.ReplicationVanillaAddon
import com.p_nsk.replicated_integration.api.addon.RegisterForgeReplicationAddonsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object ForgeBuiltinReplicationAddons {
    @SubscribeEvent
    fun onRegisterReplicationAddons(event: RegisterForgeReplicationAddonsEvent) {
        event.register(ReplicationVanillaAddon)
        event.register(ReplicationMekanismAddon)
        event.register(ReplicationAE2Addon)
        event.register(ReplicationAdvancedAEAddon)
        event.register(ReplicationDraconicAddon)
    }
}
