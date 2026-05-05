package com.p_nsk.replicated_integration.addon

import com.p_nsk.replicated_integration.api.ReplicationAddonEnvironment
import net.neoforged.fml.ModList

object NeoReplicationAddonEnvironment : ReplicationAddonEnvironment {
    override fun isModLoaded(modId: String): Boolean = ModList.get().isLoaded(modId)
}
