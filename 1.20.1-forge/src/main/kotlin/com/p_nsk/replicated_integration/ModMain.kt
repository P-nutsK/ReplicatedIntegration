package com.p_nsk.replicated_integration

import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod

@Mod(Constants.MOD_ID)
object ModMain {
    init {
        ReplicationCompatBootstrap
            .create { modId -> ModList.get().isLoaded(modId) }
            .initialize()
    }
}
