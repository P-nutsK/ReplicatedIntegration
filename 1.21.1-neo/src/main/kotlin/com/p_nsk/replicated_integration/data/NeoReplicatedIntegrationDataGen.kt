package com.p_nsk.replicated_integration.data

import net.neoforged.neoforge.data.event.GatherDataEvent

object NeoReplicatedIntegrationDataGen {
    fun gatherData(event: GatherDataEvent) {
        if (!event.includeServer()) {
            return
        }

        event.generator.addProvider(
            true,
            NeoReplicationMatterValueProvider(event.generator.packOutput),
        )
    }
}
