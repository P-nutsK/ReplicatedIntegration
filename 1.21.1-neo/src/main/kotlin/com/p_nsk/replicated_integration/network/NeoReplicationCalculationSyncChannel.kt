package com.p_nsk.replicated_integration.network

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler

object NeoReplicationCalculationSyncChannel {
    private const val PROTOCOL_VERSION = "1"

    fun register(event: RegisterPayloadHandlersEvent) {
        event.registrar(PROTOCOL_VERSION)
            .playToClient(
                NeoReplicationCalculationSyncPayload.TYPE,
                NeoReplicationCalculationSyncPayload.STREAM_CODEC,
                MainThreadPayloadHandler { payload, context -> payload.handle(context) },
            )
    }
}
