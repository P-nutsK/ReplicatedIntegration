package com.p_nsk.replicated_integration.bridge

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterCompound
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.adapter.mekanism.ReplicationMekanismAddon
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.adapter.vanilla.ReplicationVanillaAddon
import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.api.graph.ConversionGraphBuilder
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterNodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.node.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.selector.MutableMatterSelectors
import com.p_nsk.replicated_integration.api.selector.MatterSelectorMaterializer
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonRegistry
import com.p_nsk.replicated_integration.api.graph.SimpleConversionSolver
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import com.p_nsk.replicated_integration.data.ForgeMatterConfigOverrides
import com.p_nsk.replicated_integration.data.ForgeMatterRuntimeOverrides
import com.p_nsk.replicated_integration.network.ReplicationCalculationSyncChannel
import com.p_nsk.replicated_integration.network.ReplicationCalculationSyncPacket
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.HashMap
import java.util.LinkedHashMap

object ForgeReplicationCalculationService {
    private const val MAX_SYNC_ENTRIES_PER_PACKET = 96

    @Volatile
    private var latestSyncTag: CompoundTag = CompoundTag()

    @Volatile
    private var latestSyncId: Long = 0L

    @Volatile
    private var latestSyncHash: Int = 0

    private val addons =
        ReplicationAddonRegistry(
            listOf(
                ReplicationVanillaAddon,
                ReplicationMekanismAddon,
            )
        )

    fun calculate(): CalculationArtifacts? {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return null
        val context =
            ForgeReplicationAddonContext(
                recipeManager = server.recipeManager,
                registryAccess = server.registryAccess(),
                defaultMatterRecipes = com.buuz135.replication.calculation.ReplicationCalculation.DEFAULT_MATTER_RECIPE.toList(),
            )
        val activeAddons = addons.active(ForgeReplicationAddonEnvironment)
        val defaults = MutableMatterDefaults()
        val selectors = MutableMatterSelectors()
        val builder = ConversionGraphBuilder()

        Constants.LOGGER.info(
            "Replication addon calculation starting with {} default recipes and {} active addons",
            context.defaultMatterRecipes.size,
            activeAddons.joinToString(",") { it.id },
        )

        for (addon in activeAddons) {
            addon.collectDefaults(context, defaults)
            addon.collectSelectors(context, selectors)
        }
        selectors.putAll(ForgeMatterConfigOverrides.snapshot())
        selectors.putAll(ForgeMatterRuntimeOverrides.snapshot(server))

        val selectorSnapshot = selectors.snapshot()
        defaults.putAll(MatterSelectorMaterializer.materialize(selectorSnapshot, ::expandSelectorTag))
        val explicitSnapshot = defaults.snapshot()
        for (addon in activeAddons) {
            addon.collectConversions(context, builder)
        }
        val graph = builder.build()
        Constants.LOGGER.info(
            "Replication addon calculation collected {} default nodes and {} conversions",
            explicitSnapshot.size,
            graph.conversions.size,
        )

        val solved = SimpleConversionSolver().solve(graph, explicitSnapshot)
        MatterNodeDebugCache.publish(selectorSnapshot, explicitSnapshot, graph, solved)

        val compounds = LinkedHashMap<String, MatterCompound>()
        for ((node, compound) in solved.entries.sortedBy { it.key }) {
            if (node.type != MatterNodes.ITEM) {
                continue
            }
            val exported = compound.toMatterCompound() ?: continue
            val itemId = node.id.toString()
            val current = compounds[itemId]
            compounds[itemId] =
                if (current == null) {
                    exported
                } else {
                    exported.compare(current)
                }
        }

        val tag = CompoundTag()
        for ((itemId, compound) in compounds.entries.sortedBy { it.key }) {
            tag.put(itemId, compound.serializeNBT())
        }

        Constants.LOGGER.info(
            "Replication addon calculation loaded {} default recipes, resolved {} node values and exported {} item matter compounds",
            context.defaultMatterRecipes.size,
            solved.size,
            compounds.size,
        )
        return CalculationArtifacts(tag, HashMap(compounds))
    }

    fun syncToPlayers(tag: CompoundTag) {
        if (!rememberLatestSnapshot(tag)) {
            Constants.LOGGER.info("Skipping Forge replication sync because the snapshot did not change")
            return
        }
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        for (player in server.playerList.players) {
            syncToPlayer(player, tag)
        }
    }

    fun syncLatestToPlayer(player: ServerPlayer) {
        if (latestSyncTag.isEmpty) {
            return
        }
        syncToPlayer(player, latestSyncTag)
    }

    private fun syncToPlayer(player: ServerPlayer, tag: CompoundTag) {
        val syncId = latestSyncId
        val chunks = splitSyncTag(tag)
        Constants.LOGGER.info(
            "Syncing replication calculation to {} in {} packet chunk(s)",
            player.gameProfile.name,
            chunks.size,
        )
        for ((index, chunk) in chunks.withIndex()) {
            ReplicationCalculationSyncChannel.channel.send(
                PacketDistributor.PLAYER.with { player },
                ReplicationCalculationSyncPacket(
                    syncId = syncId,
                    complete = index == chunks.lastIndex,
                    tag = chunk,
                ),
            )
        }
    }

    fun nodeTypes() = addons.nodeTypes()

    @Synchronized
    private fun rememberLatestSnapshot(tag: CompoundTag): Boolean {
        val snapshot = tag.copy()
        val snapshotHash = snapshot.hashCode()
        if (snapshotHash == latestSyncHash && snapshot == latestSyncTag) {
            return false
        }
        latestSyncTag = snapshot
        latestSyncHash = snapshotHash
        latestSyncId += 1L
        return true
    }

    private fun expandSelectorTag(type: LiteResourceLocation, id: LiteResourceLocation): Iterable<MatterNodeKey> {
        val resourceId = id.toMcResourceLocation()
        return when (type) {
            MatterNodes.ITEM -> BuiltinNodeResolver.itemNodesInTag(resourceId)
            MatterNodes.FLUID -> BuiltinNodeResolver.fluidNodesInTag(resourceId)
            else -> emptyList()
        }
    }

    private fun LiteMatterCompound.toMatterCompound(): MatterCompound? {
        val registry = ReplicationRegistry.MATTER_TYPES_REGISTRY.get()
        val compound = MatterCompound()
        for ((matterId, amount) in values.entries.sortedBy { it.key.toString() }) {
            val type = registry.getValue(matterId.toMcResourceLocation()) ?: continue
            compound.add(MatterValue(type, amount))
        }
        return if (compound.values.isEmpty()) null else compound
    }

    private fun MatterValueRecipe.toMatterCompound(): MatterCompound? {
        val compound = MatterCompound()
        for (value in matter) {
            compound.add(MatterValue(value.matter, value.amount))
        }
        return if (compound.values.isEmpty()) null else compound
    }

    private fun MatterCompound.copyOf(): MatterCompound =
        MatterCompound().also { copy ->
            for (value in values.values) {
                copy.add(MatterValue(value.matter, value.amount))
            }
        }

    private fun LiteResourceLocation.toMcResourceLocation() =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path)

    private fun splitSyncTag(tag: CompoundTag): List<CompoundTag> {
        if (tag.isEmpty) {
            return listOf(CompoundTag())
        }
        val chunks = mutableListOf<CompoundTag>()
        var current = CompoundTag()
        var entries = 0
        for (key in tag.allKeys.sorted()) {
            val value = tag.get(key)?.copy() ?: continue
            if (entries >= MAX_SYNC_ENTRIES_PER_PACKET) {
                chunks += current
                current = CompoundTag()
                entries = 0
            }
            current.put(key, value)
            entries += 1
        }
        if (!current.isEmpty) {
            chunks += current
        }
        return chunks
    }
}
