package com.p_nsk.replicated_integration.core

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterCompound
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.calculation.ReplicationCalculation
import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.adapter.mekanism.MekanismNodeResolver
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.api.graph.ConversionGraphBuilder
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.node.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.selector.MutableMatterSelectors
import com.p_nsk.replicated_integration.api.selector.MatterSelectorMaterializer
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonRegistry
import com.p_nsk.replicated_integration.api.graph.SimpleConversionSolver
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import com.p_nsk.replicated_integration.data.NeoMatterConfigOverrides
import com.p_nsk.replicated_integration.data.NeoMatterRuntimeOverrides
import com.p_nsk.replicated_integration.network.NeoReplicationCalculationSyncPayload
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.server.ServerLifecycleHooks
import java.util.HashMap
import java.util.LinkedHashMap

@OptIn(ReplicationAddonLoadSafetyContract::class)
object NeoReplicationCalculationService {
    private const val MAX_SYNC_ENTRIES_PER_PACKET = 96

    @Volatile
    private var latestSyncTag: CompoundTag = CompoundTag()

    @Volatile
    private var latestSyncHash: Int = 0

    @Volatile
    private var latestSyncId: Long = 0L

    private val addons: ReplicationAddonRegistry<NeoReplicationAddonContext, NeoMatterCommand> by lazy {
        ReplicationAddonRegistry(NeoReplicationAddonCollector.addons())
    }

    fun prepareSnapshot(server: MinecraftServer): NeoCalculationSnapshot {
        NeoRecipeConversionSupport.invalidateCache()
        val context =
            NeoReplicationAddonContext(
                recipeManager = server.recipeManager,
                registryAccess = server.registryAccess(),
                defaultMatterRecipes = ReplicationCalculation.DEFAULT_MATTER_RECIPE.toList(),
            )
        val activeAddons = addons.active(NeoReplicationAddonEnvironment)
        val defaults = MutableMatterDefaults()
        val selectors = MutableMatterSelectors()
        val builder = ConversionGraphBuilder()

        for (addon in activeAddons) {
            addon.collectDefaults(context, defaults)
            addon.collectSelectors(context, selectors)
        }
        selectors.putAll(NeoMatterConfigOverrides.snapshot())
        selectors.putAll(NeoMatterRuntimeOverrides.snapshot(server))

        val selectorSnapshot = selectors.snapshot()
        defaults.putAll(MatterSelectorMaterializer.materialize(selectorSnapshot, ::expandSelectorTag))
        val explicitSnapshot = defaults.snapshot()
        for (addon in activeAddons) {
            addon.collectConversions(context, builder)
        }
        return NeoCalculationSnapshot(
            defaultMatterRecipeCount = context.defaultMatterRecipes.size,
            activeAddonIds = activeAddons.map { it.id },
            selectorSnapshot = selectorSnapshot,
            explicitSnapshot = explicitSnapshot,
            graph = builder.build(),
            registryAccess = context.registryAccess,
        )
    }

    fun calculate(snapshot: NeoCalculationSnapshot): CalculationArtifacts {
        Constants.LOGGER.info(
            "Replication addon calculation starting with {} default recipes and {} active addons",
            snapshot.defaultMatterRecipeCount,
            snapshot.activeAddonIds.joinToString(","),
        )
        Constants.LOGGER.info(
            "Replication addon calculation collected {} default nodes and {} conversions",
            snapshot.explicitSnapshot.size,
            snapshot.graph.conversions.size,
        )

        val solved = SimpleConversionSolver().solve(snapshot.graph, snapshot.explicitSnapshot)
        MatterNodeDebugCache.publish(snapshot.selectorSnapshot, snapshot.explicitSnapshot, snapshot.graph, solved)

        val compounds = LinkedHashMap<Item, MatterCompound>()
        for ((node, compound) in solved.entries.sortedBy { it.key }) {
            if (node.type != MatterNodes.ITEM) {
                continue
            }
            val exported = compound.toMatterCompound() ?: continue
            val itemId = node.id.toMcResourceLocation()
            val item = BuiltInRegistries.ITEM.get(itemId)
            if (item == net.minecraft.world.item.Items.AIR) {
                continue
            }
            val current = compounds[item]
            compounds[item] =
                if (current == null) {
                    exported
                } else {
                    exported.compare(current)
                }
        }

        val tag = CompoundTag()
        for ((item, compound) in compounds.entries.sortedBy { BuiltInRegistries.ITEM.getKey(it.key).toString() }) {
            val itemId = BuiltInRegistries.ITEM.getKey(item)
            tag.put(itemId.toString(), compound.serializeNBT(snapshot.registryAccess))
        }

        Constants.LOGGER.info(
            "Replication addon calculation loaded {} default recipes, resolved {} node values and exported {} item matter compounds",
            snapshot.defaultMatterRecipeCount,
            solved.size,
            compounds.size,
        )
        return CalculationArtifacts(tag, HashMap(compounds))
    }

    fun syncToPlayers(tag: CompoundTag) {
        if (!rememberLatestSnapshot(tag)) {
            Constants.LOGGER.info("Skipping Neo replication sync because the snapshot did not change")
            return
        }
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        for (player in server.playerList.players) {
            syncToPlayer(player, tag)
        }
    }

    private fun matterNodes(): NeoMatterNodeRegistry =
        addons.matterNodes(NeoReplicationAddonEnvironment)

    fun matterNodeRegistry(): NeoMatterNodeRegistry =
        matterNodes()

    fun commandTargets(): List<NeoMatterCommand> =
        matterNodes().commands()

    fun nodeTypes(): NeoMatterNodeRegistry =
        matterNodes()

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

    private fun expandSelectorTag(type: LiteResourceLocation, id: LiteResourceLocation): Iterable<NodeKey> {
        val resourceId = id.toMcResourceLocation()
        return when (type) {
            MatterNodes.ITEM -> BuiltinNodeResolver.itemNodesInTag(resourceId)
            MatterNodes.FLUID -> BuiltinNodeResolver.fluidNodesInTag(resourceId)
            MatterNodes.CHEMICAL -> MekanismNodeResolver.chemicalNodesInTag(resourceId)
            else -> emptyList()
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
        for ((index, chunk) in chunks.withIndex()) {
            PacketDistributor.sendToPlayer(
                player,
                NeoReplicationCalculationSyncPayload(
                    syncId = syncId,
                    complete = index == chunks.lastIndex,
                    tag = chunk,
                ),
            )
        }
    }

    private fun LiteMatterCompound.toMatterCompound(): MatterCompound? {
        val registry = ReplicationRegistry.MATTER_TYPES_REGISTRY ?: return null
        val compound = MatterCompound()
        for ((matterId, amount) in values.entries.sortedBy { it.key.toString() }) {
            val type = registry.get(matterId.toMcResourceLocation()) ?: continue
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

    private fun LiteResourceLocation.toMcResourceLocation(): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, path)

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
