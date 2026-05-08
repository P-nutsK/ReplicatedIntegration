package com.p_nsk.replicated_integration.network

import com.buuz135.replication.calculation.client.ClientReplicationCalculation
import com.p_nsk.replicated_integration.Constants
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

data class NeoReplicationCalculationSyncPayload(
    val syncId: Long,
    val complete: Boolean,
    val tag: CompoundTag,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<NeoReplicationCalculationSyncPayload> = TYPE

    fun handle(context: IPayloadContext) {
        context.enqueueWork {
            ClientSyncAccumulator.accept(syncId, complete, tag)
        }
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
                val connection = Minecraft.getInstance().connection ?: return@execute
                ClientReplicationCalculation.acceptData(connection.registryAccess(), finalTag)
            }
        }
    }

    companion object {
        val TYPE = CustomPacketPayload.Type<NeoReplicationCalculationSyncPayload>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "replication_sync"),
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, NeoReplicationCalculationSyncPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, NeoReplicationCalculationSyncPayload> {
                override fun decode(buffer: RegistryFriendlyByteBuf): NeoReplicationCalculationSyncPayload =
                    NeoReplicationCalculationSyncPayload(
                        syncId = buffer.readVarLong(),
                        complete = buffer.readBoolean(),
                        tag = buffer.readNbt() ?: CompoundTag(),
                    )

                override fun encode(buffer: RegistryFriendlyByteBuf, value: NeoReplicationCalculationSyncPayload) {
                    buffer.writeVarLong(value.syncId)
                    buffer.writeBoolean(value.complete)
                    buffer.writeNbt(value.tag)
                }
            }
    }
}
