package com.p_nsk.replicated_integration

import com.p_nsk.replicated_integration.command.MatterCommand
import com.p_nsk.replicated_integration.config.NeoCompatibilityConfig
import com.p_nsk.replicated_integration.data.ReplicationReloadHooks
import com.p_nsk.replicated_integration.data.ReplicationServerLifecycleHooks
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig

@Mod(Constants.MOD_ID)
object ModMain {
    init {
        registerConfigs()
        NeoForge.EVENT_BUS.register(MatterCommand)
        NeoForge.EVENT_BUS.register(ReplicationReloadHooks)
        NeoForge.EVENT_BUS.register(ReplicationServerLifecycleHooks)
    }

    private fun registerConfigs() {
        ModLoadingContext.get().activeContainer.registerConfig(
            ModConfig.Type.COMMON,
            NeoCompatibilityConfig.spec,
            "replicated_integration-compatibility.toml",
        )
    }
}
