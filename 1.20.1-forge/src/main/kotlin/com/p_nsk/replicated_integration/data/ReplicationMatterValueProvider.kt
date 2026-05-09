package com.p_nsk.replicated_integration.data

import appeng.core.definitions.AEItems
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.p_nsk.replicated_integration.Constants
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class ReplicationMatterValueProvider(
    output: PackOutput,
) : DataProvider {
    private val pathProvider =
        output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/matter_values")

    private val nodeValuePathProvider =
        output.createPathProvider(PackOutput.Target.DATA_PACK, "replicated_integration/matter_node_values")

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val writes = linkedMapOf<Path, JsonObject>()
        val out = MatterValueWriter(writes)

        backports(out)
        mekanismValues(out)
        ae2Values(out)
        fluidValues(out)

        return CompletableFuture.allOf(
            *writes.entries.map { (path, json) ->
                DataProvider.saveStable(cache, json, path)
            }.toTypedArray(),
        )
    }

    override fun getName(): String =
        "Replicated Integration Replication Matter Value Supplements"

    private fun backports(out: MatterValueWriter) {
        // Vanilla / common tag values backported from newer Replication data.
        out.items(
            listOf(
                Items.OCHRE_FROGLIGHT,
                Items.PEARLESCENT_FROGLIGHT,
                Items.VERDANT_FROGLIGHT,
            ),
            matter(living = 2.0, organic = 2.0),
        )

        out.forgeTag("ingots/osmium", matter(metallic = 9.0, precious = 9.0))
        out.forgeTag("ingots/aluminum", matter(metallic = 9.0, precious = 9.0))
        out.forgeTag("ingots/antimony", matter(metallic = 9.0))
        out.forgeTag("ingots/lead", matter(metallic = 9.0))
        out.forgeTag("ingots/iridium", matter(metallic = 9.0, precious = 9.0, quantum = 9.0))
        out.forgeTag("ingots/nickel", matter(metallic = 9.0))
        out.forgeTag("ingots/platinum", matter(metallic = 9.0, precious = 18.0))
        out.forgeTag("ingots/plutonium", matter(metallic = 9.0, precious = 18.0, quantum = 18.0))
        out.forgeTag("ingots/iesnium", matter(metallic = 9.0, quantum = 9.0))
        out.forgeTag("ingots/silver", matter(metallic = 9.0, precious = 9.0))
        out.forgeTag("ingots/tin", matter(metallic = 9.0))
        out.forgeTag("ingots/titanium", matter(metallic = 9.0, precious = 9.0))
        out.forgeTag("ingots/tungsten", matter(metallic = 18.0, precious = 9.0))
        out.forgeTag("ingots/uranium", matter(metallic = 9.0, quantum = 9.0))
        out.forgeTag("ingots/uraninite", matter(metallic = 9.0, quantum = 9.0))
        out.forgeTag("ingots/mithril", matter(metallic = 9.0, precious = 18.0))

        out.forgeTag("plastics", matter(organic = 9.0, precious = 2.0))
        out.forgeTag("cork", matter(organic = 2.0))

        out.forgeTag("gems/dark", matter(precious = 24.0))
        out.forgeTag("dusts/dark", matter(precious = 12.0))
        out.forgeTag("silicon", matter(earth = 2.0))

        out.forgeTag("berries", matter(earth = 4.0, organic = 4.0))
        out.forgeTag("fruits", matter(earth = 4.0, organic = 4.0))
        out.forgeTag("nuts", matter(earth = 4.0, organic = 4.0))
        out.forgeTag("food/berry", matter(earth = 4.0, organic = 4.0))
        out.forgeTag("crops", matter(earth = 2.0, organic = 2.0))
    }

    private fun mekanismValues(out: MatterValueWriter) {
        out.forgeTag("gems/fluorite", matter(precious = 4.0, earth = 3.0))
    }

    private fun ae2Values(out: MatterValueWriter) {
        out.item(AEItems.MATTER_BALL.asItem(), matter(earth = 256.0))
        out.item(AEItems.SKY_DUST.asItem(), matter(earth = 1.0))
        out.forgeTag("gems/certus_quartz", matter(precious = 3.0, earth = 1.0))
    }

    private fun fluidValues(out: MatterValueWriter) {
        // Fluid node values are per mB.
        // Write bucket-scale values here and divide by 1000 via perBucket(...).
        out.fluid("minecraft:water", perBucket(earth = 1.0))
        out.fluid("minecraft:lava", perBucket(earth = 4.0, nether = 1.0))
    }

    private inner class MatterValueWriter(
        private val writes: MutableMap<Path, JsonObject>,
    ) {
        fun item(
            item: Item,
            matter: Matter,
        ) {
            if (item == Items.AIR) return

            item(BuiltInRegistries.ITEM.getKey(item), matter)
        }

        fun item(
            itemId: String,
            matter: Matter,
        ) {
            item(ResourceLocation.parse(itemId), matter)
        }

        fun item(
            itemId: ResourceLocation,
            matter: Matter,
        ) {
            val path = pathProvider.json(
                ResourceLocation.fromNamespaceAndPath(
                    Constants.MOD_ID,
                    "${itemId.namespace}/items/${itemId.path}",
                ),
            )
            writes[path] = matterValueRecipeJson(itemId.toString(), isTag = false, matter)
        }

        fun items(
            items: Iterable<Item>,
            matter: Matter,
        ) {
            items.forEach { item(it, matter) }
        }

        fun tag(
            tag: TagKey<Item>,
            matter: Matter,
        ) {
            tag(tag.location(), matter)
        }

        fun tag(
            tagId: String,
            matter: Matter,
        ) {
            tag(ResourceLocation.parse(tagId), matter)
        }

        fun tag(
            tagId: ResourceLocation,
            matter: Matter,
        ) {
            val path = pathProvider.json(
                ResourceLocation.fromNamespaceAndPath(
                    Constants.MOD_ID,
                    "${tagId.namespace}/tags/${tagId.path}",
                ),
            )
            writes[path] = matterValueRecipeJson(tagId.toString(), isTag = true, matter)
        }

        fun forgeTag(
            path: String,
            matter: Matter,
        ) {
            tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", path)), matter)
        }

        fun fluid(
            fluidId: String,
            matter: Matter,
        ) {
            fluid(ResourceLocation.parse(fluidId), matter)
        }

        fun fluid(
            fluidId: ResourceLocation,
            matter: Matter,
        ) {
            val path = nodeValuePathProvider.json(
                ResourceLocation.fromNamespaceAndPath(
                    Constants.MOD_ID,
                    "${fluidId.namespace}/fluids/${fluidId.path}",
                ),
            )
            writes[path] = matterNodeValueJson("c:fluid", fluidId.toString(), matter)
        }
    }

    private fun matterValueRecipeJson(
        inputId: String,
        isTag: Boolean,
        matter: Matter,
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("type", "replication:matter_value")

        val input = JsonObject()
        input.addProperty(if (isTag) "tag" else "item", inputId)
        root.add("input", input)

        root.add("matter", matter.toJsonArray())

        return root
    }

    private fun matterNodeValueJson(
        typeId: String,
        nodeId: String,
        matter: Matter,
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("type", typeId)
        root.addProperty("id", nodeId)
        root.add("matter", matter.toJsonArray())

        return root
    }

    private fun Matter.toJsonArray(): JsonArray {
        val matterArray = JsonArray()

        for (entry in entries) {
            val element = JsonObject()
            element.addProperty("type", "replication:${entry.type}")
            element.addProperty("value", entry.value)
            matterArray.add(element)
        }

        return matterArray
    }

    private fun perBucket(
        metallic: Double = 0.0,
        earth: Double = 0.0,
        organic: Double = 0.0,
        quantum: Double = 0.0,
        nether: Double = 0.0,
        precious: Double = 0.0,
        ender: Double = 0.0,
        living: Double = 0.0,
    ): Matter =
        matter(
            metallic = metallic / BUCKET_VOLUME,
            earth = earth / BUCKET_VOLUME,
            organic = organic / BUCKET_VOLUME,
            quantum = quantum / BUCKET_VOLUME,
            nether = nether / BUCKET_VOLUME,
            precious = precious / BUCKET_VOLUME,
            ender = ender / BUCKET_VOLUME,
            living = living / BUCKET_VOLUME,
        )

    private fun matter(
        metallic: Double = 0.0,
        earth: Double = 0.0,
        organic: Double = 0.0,
        quantum: Double = 0.0,
        nether: Double = 0.0,
        precious: Double = 0.0,
        ender: Double = 0.0,
        living: Double = 0.0,
    ): Matter =
        Matter(
            buildList {
                addIfPositive("metallic", metallic)
                addIfPositive("earth", earth)
                addIfPositive("organic", organic)
                addIfPositive("quantum", quantum)
                addIfPositive("nether", nether)
                addIfPositive("precious", precious)
                addIfPositive("ender", ender)
                addIfPositive("living", living)
            },
        )

    private fun MutableList<MatterEntry>.addIfPositive(
        type: String,
        value: Double,
    ) {
        if (value > 0.0) {
            add(MatterEntry(type, value))
        }
    }

    private data class Matter(
        val entries: List<MatterEntry>,
    )

    private data class MatterEntry(
        val type: String,
        val value: Double,
    )

    private companion object {
        private const val BUCKET_VOLUME = 1000.0
    }
}
