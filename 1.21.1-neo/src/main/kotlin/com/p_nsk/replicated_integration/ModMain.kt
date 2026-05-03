package com.p_nsk.replicated_integration

import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod

@Mod(Constants.MOD_ID)
object ModMain {
    init {
        ReplicationCompatBootstrap
            .create { modId -> ModList.get().isLoaded(modId) }
            .initialize()
    }
}
