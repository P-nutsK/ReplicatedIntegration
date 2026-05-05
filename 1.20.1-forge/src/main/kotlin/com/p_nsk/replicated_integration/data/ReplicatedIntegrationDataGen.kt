package com.p_nsk.replicated_integration.data

import net.minecraftforge.data.event.GatherDataEvent

object ReplicatedIntegrationDataGen {
    fun gatherData(event: GatherDataEvent) {
        if (!event.includeServer()) {
            return
        }
        event.generator.addProvider(
            true,
            ReplicationMatterValueProvider(event.generator.packOutput),
        )
    }
}
