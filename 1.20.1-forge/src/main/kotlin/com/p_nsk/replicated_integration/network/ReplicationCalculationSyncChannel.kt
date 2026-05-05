package com.p_nsk.replicated_integration.network

import com.p_nsk.replicated_integration.Constants
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel

object ReplicationCalculationSyncChannel {
    private const val PROTOCOL_VERSION = "2"

    val channel: SimpleChannel =
        NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "replication_sync"),
            { PROTOCOL_VERSION },
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals,
        )

    fun register() {
        channel.registerMessage(
            0,
            ReplicationCalculationSyncPacket::class.java,
            ReplicationCalculationSyncPacket::encode,
            ::ReplicationCalculationSyncPacket,
            ReplicationCalculationSyncPacket::handle,
        )
    }
}
