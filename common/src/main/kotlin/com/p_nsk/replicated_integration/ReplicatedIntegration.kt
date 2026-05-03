package com.p_nsk.replicated_integration

import com.p_nsk.replicated_integration.compat.CompatContext
import com.p_nsk.replicated_integration.compat.CompatModule
import com.p_nsk.replicated_integration.compat.CompatRegistry
import com.p_nsk.replicated_integration.compat.PlatformModLookup
import com.p_nsk.replicated_integration.replication.NoOpReplicationBridge
import com.p_nsk.replicated_integration.replication.ReplicationBridge

class ReplicatedIntegration private constructor(
    private val platformName: String,
    private val compatRegistry: CompatRegistry,
    private val replicationBridge: ReplicationBridge,
    private val modLookup: PlatformModLookup,
) {
    fun initialize() {
        Constants.LOGGER.info("Initializing {} on {}", Constants.MOD_NAME, platformName)
        compatRegistry.initialize(
            CompatContext(
                platformName = platformName,
                modLookup = modLookup,
                replicationBridge = replicationBridge,
            )
        )
    }

    companion object {
        fun create(
            platformName: String,
            modLookup: PlatformModLookup,
            modules: Iterable<CompatModule>,
            replicationBridge: ReplicationBridge = NoOpReplicationBridge,
        ): ReplicatedIntegration {
            val registry = CompatRegistry()
            modules.forEach(registry::register)
            return ReplicatedIntegration(platformName, registry, replicationBridge, modLookup)
        }
    }
}
