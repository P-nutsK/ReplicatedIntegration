package com.p_nsk.replicated_integration.network

import com.buuz135.replication.calculation.client.ClientReplicationCalculation
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class ReplicationCalculationSyncPacket(
    val syncId: Long,
    val complete: Boolean,
    val tag: CompoundTag,
) {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readVarLong(),
        buf.readBoolean(),
        buf.readNbt() ?: CompoundTag(),
    )

    fun encode(buf: FriendlyByteBuf) {
        buf.writeVarLong(syncId)
        buf.writeBoolean(complete)
        buf.writeNbt(tag)
    }

    fun handle(contextSupplier: Supplier<NetworkEvent.Context>) {
        val context = contextSupplier.get()
        context.enqueueWork {
            ClientSyncAccumulator.accept(syncId, complete, tag)
        }
        context.packetHandled = true
    }

    private object ClientSyncAccumulator {
        private var pendingSyncId: Long = Long.MIN_VALUE
        private var pendingTag: CompoundTag = CompoundTag()

        fun accept(syncId: Long, complete: Boolean, chunk: CompoundTag) {
            if (pendingSyncId != syncId) {
                pendingSyncId = syncId
                pendingTag = CompoundTag()
            }
            for (key in chunk.allKeys.sorted()) {
                val value = chunk.get(key)?.copy() ?: continue
                pendingTag.put(key, value)
            }
            if (!complete) {
                return
            }
            val finalTag = pendingTag.copy()
            pendingSyncId = Long.MIN_VALUE
            pendingTag = CompoundTag()
            Minecraft.getInstance().execute {
                ClientReplicationCalculation.acceptData(finalTag)
            }
        }
    }

    companion object {
        fun encode(
            packet: ReplicationCalculationSyncPacket,
            buf: FriendlyByteBuf,
        ) = packet.encode(buf)

        fun handle(
            packet: ReplicationCalculationSyncPacket,
            contextSupplier: Supplier<NetworkEvent.Context>,
        ) = packet.handle(contextSupplier)
    }
}
