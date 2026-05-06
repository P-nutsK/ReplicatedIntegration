

package com.p_nsk.replicated_integration

import com.p_nsk.replicated_integration.command.MatterNodeDebugCommand
import com.p_nsk.replicated_integration.command.MatterCommand
import com.p_nsk.replicated_integration.config.ForgeCompatibilityConfig
import com.p_nsk.replicated_integration.data.ReplicatedIntegrationDataGen
import com.p_nsk.replicated_integration.data.ReplicationReloadHooks
import com.p_nsk.replicated_integration.data.ReplicationServerLifecycleHooks
import com.p_nsk.replicated_integration.network.ReplicationCalculationSyncChannel
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
@Suppress("removal")
@Mod(Constants.MOD_ID)
object ModMain {
    init {
        registerKotlinModEventListeners()
        registerConfigs()
        ReplicationCalculationSyncChannel.register()
        MinecraftForge.EVENT_BUS.register(MatterCommand)
        MinecraftForge.EVENT_BUS.register(MatterNodeDebugCommand)
        MinecraftForge.EVENT_BUS.register(ReplicationReloadHooks)
        MinecraftForge.EVENT_BUS.register(ReplicationServerLifecycleHooks)
    }

    @Suppress("DEPRECATION")
    private fun registerKotlinModEventListeners() {
        val extension: Any = ModLoadingContext.get().extension()
        val method = extension::class.java.getMethod("getKEventBus")
        val eventBus = method.invoke(extension) as IEventBus
        eventBus.addListener(ReplicatedIntegrationDataGen::gatherData)
    }

    private fun registerConfigs() {
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.COMMON,
            ForgeCompatibilityConfig.spec,
            "replicated_integration-compatibility.toml",
        )
    }
}
