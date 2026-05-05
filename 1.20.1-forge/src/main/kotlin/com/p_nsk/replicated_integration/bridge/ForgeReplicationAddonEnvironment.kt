package com.p_nsk.replicated_integration.bridge

import com.p_nsk.replicated_integration.api.ReplicationAddonEnvironment
import net.minecraftforge.fml.ModList

object ForgeReplicationAddonEnvironment : ReplicationAddonEnvironment {
    override fun isModLoaded(modId: String): Boolean = ModList.get().isLoaded(modId)
}
