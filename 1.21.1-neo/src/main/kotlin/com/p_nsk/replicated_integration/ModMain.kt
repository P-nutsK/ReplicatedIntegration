package com.p_nsk.replicated_integration

import com.p_nsk.replicated_integration.data.ReplicationReloadHooks
import com.p_nsk.replicated_integration.data.ReplicationServerLifecycleHooks
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.fml.common.Mod

@Mod(Constants.MOD_ID)
object ModMain {
    init {
        NeoForge.EVENT_BUS.register(ReplicationReloadHooks)
        NeoForge.EVENT_BUS.register(ReplicationServerLifecycleHooks)
    }
}
