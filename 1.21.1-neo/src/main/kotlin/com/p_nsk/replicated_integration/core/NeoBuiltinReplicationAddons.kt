package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.adapter.mekanism.ReplicationMekanismAddon
import com.p_nsk.replicated_integration.adapter.vanilla.ReplicationVanillaAddon
import com.p_nsk.replicated_integration.api.addon.RegisterNeoReplicationAddonsEvent
import net.neoforged.bus.api.SubscribeEvent

object NeoBuiltinReplicationAddons {
    @SubscribeEvent
    fun onRegisterReplicationAddons(event: RegisterNeoReplicationAddonsEvent) {
        event.register(ReplicationVanillaAddon)
        event.register(ReplicationMekanismAddon)
    }
}
