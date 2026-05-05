package com.p_nsk.replicated_integration.addon

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterCompound
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.buuz135.replication.packet.ReplicationCalculationPacket
import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.api.ConversionGraphBuilder
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterNodes
import com.p_nsk.replicated_integration.api.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.ReplicationAddonRegistry
import com.p_nsk.replicated_integration.api.SimpleConversionSolver
import com.p_nsk.replicated_integration.debug.MatterNodeDebugCache
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.HashMap
import java.util.LinkedHashMap

object ForgeReplicationCalculationService {
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
        val builder = ConversionGraphBuilder()

        Constants.LOGGER.info(
            "Replication addon calculation starting with {} default recipes and {} active addons",
            context.defaultMatterRecipes.size,
            activeAddons.joinToString(",") { it.id },
        )

        for (addon in activeAddons) {
            addon.collectDefaults(context, defaults)
            addon.collectConversions(context, builder)
        }

        val defaultSnapshot = defaults.snapshot()
        val graph = builder.build()
        Constants.LOGGER.info(
            "Replication addon calculation collected {} default nodes and {} conversions",
            defaultSnapshot.size,
            graph.conversions.size,
        )

        val solved = SimpleConversionSolver().solve(graph, defaultSnapshot)
        MatterNodeDebugCache.publish(defaultSnapshot, graph, solved)

        val compounds = LinkedHashMap<String, MatterCompound>()
        val seededDefaults = seedDefaultCompounds(context.defaultMatterRecipes, compounds)
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
            "Replication addon calculation loaded {} default recipes, seeded {} default items, resolved {} node values and exported {} item matter compounds",
            context.defaultMatterRecipes.size,
            seededDefaults,
            solved.size,
            compounds.size,
        )
        return CalculationArtifacts(tag, HashMap(compounds))
    }

    fun syncToPlayers(tag: CompoundTag) {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        for (player in server.playerList.players) {
            com.buuz135.replication.Replication.NETWORK.get()
                .sendTo(
                    ReplicationCalculationPacket(tag),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT,
                )
        }
    }

    fun nodeTypes() = addons.nodeTypes()

    private fun LiteMatterCompound.toMatterCompound(): MatterCompound? {
        val registry = ReplicationRegistry.MATTER_TYPES_REGISTRY.get()
        val compound = MatterCompound()
        for ((matterId, amount) in values.entries.sortedBy { it.key.toString() }) {
            val type = registry.getValue(matterId.toMcResourceLocation()) ?: continue
            compound.add(MatterValue(type, amount))
        }
        return if (compound.values.isEmpty()) null else compound
    }

    private fun seedDefaultCompounds(
        recipes: List<MatterValueRecipe>,
        compounds: MutableMap<String, MatterCompound>,
    ): Int {
        var count = 0
        for (recipe in recipes) {
            val compound = recipe.toMatterCompound() ?: continue
            for (stack in recipe.input.items) {
                val itemId = stack.toItemIdOrNull() ?: continue
                val current = compounds[itemId]
                compounds[itemId] =
                    if (current == null) {
                        count++
                        compound.copyOf()
                    } else {
                        compound.copyOf().compare(current)
                    }
            }
        }
        return count
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

    private fun ItemStack.toItemIdOrNull(): String? =
        if (isEmpty) {
            null
        } else {
            BuiltInRegistries.ITEM.getKey(item).toString()
        }

    private fun LiteResourceLocation.toMcResourceLocation() =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path)
}
