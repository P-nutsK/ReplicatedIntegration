package com.p_nsk.replicated_integration.data

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
    private val pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/matter_values")
    private val nodeValuePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "replicated_integration/matter_node_values")

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val writes = linkedMapOf<Path, JsonObject>()

        saveData(
            writes,
            arrayOf(Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, Items.VERDANT_FROGLIGHT),
            matter(living = 2.0, organic = 2.0),
        )

        saveForgeTag(writes, "ingots/osmium", matter(metallic = 9.0, precious = 9.0))
        saveForgeTag(writes, "ingots/aluminum", matter(metallic = 9.0, precious = 9.0))
        saveForgeTag(writes, "ingots/antimony", matter(metallic = 9.0))
        saveForgeTag(writes, "ingots/lead", matter(metallic = 9.0))
        saveForgeTag(writes, "ingots/iridium", matter(metallic = 9.0, precious = 9.0, quantum = 9.0))
        saveForgeTag(writes, "ingots/nickel", matter(metallic = 9.0))
        saveForgeTag(writes, "ingots/platinum", matter(metallic = 9.0, precious = 18.0))
        saveForgeTag(writes, "ingots/plutonium", matter(metallic = 9.0, precious = 18.0, quantum = 18.0))
        saveForgeTag(writes, "ingots/iesnium", matter(metallic = 9.0, quantum = 9.0))
        saveForgeTag(writes, "ingots/silver", matter(metallic = 9.0, precious = 9.0))
        saveForgeTag(writes, "ingots/tin", matter(metallic = 9.0))
        saveForgeTag(writes, "ingots/titanium", matter(metallic = 9.0, precious = 9.0))
        saveForgeTag(writes, "ingots/tungsten", matter(metallic = 18.0, precious = 9.0))
        saveForgeTag(writes, "ingots/uranium", matter(metallic = 9.0, quantum = 9.0))
        saveForgeTag(writes, "ingots/uraninite", matter(metallic = 9.0, quantum = 9.0))
        saveForgeTag(writes, "ingots/mithril", matter(metallic = 9.0, precious = 18.0))
        saveForgeTag(writes, "plastics", matter(organic = 9.0, precious = 2.0))

        saveForgeTag(writes, "cork", matter(organic = 2.0))
        saveForgeTag(writes, "gems/dark", matter(precious = 24.0))
        saveForgeTag(writes, "dusts/dark", matter(precious = 12.0))
        saveForgeTag(writes, "silicon", matter(earth = 2.0))

        saveForgeTag(writes, "berries", matter(earth = 4.0, organic = 4.0))
        saveForgeTag(writes, "fruits", matter(earth = 4.0, organic = 4.0))
        saveForgeTag(writes, "nuts", matter(earth = 4.0, organic = 4.0))
        saveForgeTag(writes, "food/berry", matter(earth = 4.0, organic = 4.0))
        saveForgeTag(writes, "crops", matter(earth = 2.0, organic = 2.0))


        // 自作
        saveForgeTag(writes, "gems/fluorite", matter(precious = 4.0, earth = 1.0))
        saveFluidNodeValue(
            writes,
            ResourceLocation.fromNamespaceAndPath("minecraft", "water"),
            matter(earth = 0.001),
        )
        return CompletableFuture.allOf(
            *writes.entries.map { (path, json) ->
                DataProvider.saveStable(cache, json, path)
            }.toTypedArray()
        )
    }

    override fun getName(): String =
        "Replicated Integration Replication Matter Value Supplements"

    private fun saveData(
        writes: MutableMap<Path, JsonObject>,
        item: Item,
        matter: List<MatterEntry>,
    ) {
        if (item == Items.AIR) {
            return
        }
        val itemId = BuiltInRegistries.ITEM.getKey(item)
        val path = pathProvider.json(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "${itemId.namespace}/items/${itemId.path}"))
        writes[path] = matterValueRecipeJson(itemId.toString(), false, matter)
    }

    private fun saveData(
        writes: MutableMap<Path, JsonObject>,
        items: Array<Item>,
        matter: List<MatterEntry>,
    ) {
        items.forEach { saveData(writes, it, matter) }
    }

    private fun saveTag(
        writes: MutableMap<Path, JsonObject>,
        tag: TagKey<Item>,
        matter: List<MatterEntry>,
    ) {
        val tagId = tag.location()
        val path = pathProvider.json(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "${tagId.namespace}/tags/${tagId.path}"))
        writes[path] = matterValueRecipeJson(tagId.toString(), true, matter)
    }

    private fun saveForgeTag(
        writes: MutableMap<Path, JsonObject>,
        path: String,
        matter: List<MatterEntry>,
    ) {
        saveTag(writes, ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", path)), matter)
    }

    private fun saveFluidNodeValue(
        writes: MutableMap<Path, JsonObject>,
        fluidId: ResourceLocation,
        matter: List<MatterEntry>,
    ) {
        val path =
            nodeValuePathProvider.json(
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "${fluidId.namespace}/fluids/${fluidId.path}")
            )
        writes[path] = matterNodeValueJson("c:fluid", fluidId.toString(), matter)
    }

    private fun matterValueRecipeJson(
        inputId: String,
        isTag: Boolean,
        matter: List<MatterEntry>,
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("type", "replication:matter_value")

        val input = JsonObject()
        input.addProperty(if (isTag) "tag" else "item", inputId)
        root.add("input", input)

        val matterArray = JsonArray()
        for (entry in matter) {
            val element = JsonObject()
            element.addProperty("type", "replication:${entry.type}")
            element.addProperty("value", entry.value)
            matterArray.add(element)
        }
        root.add("matter", matterArray)
        return root
    }

    private fun matterNodeValueJson(
        typeId: String,
        nodeId: String,
        matter: List<MatterEntry>,
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("type", typeId)
        root.addProperty("id", nodeId)

        val matterArray = JsonArray()
        for (entry in matter) {
            val element = JsonObject()
            element.addProperty("type", "replication:${entry.type}")
            element.addProperty("value", entry.value)
            matterArray.add(element)
        }
        root.add("matter", matterArray)
        return root
    }

    private fun matter(
        metallic: Double = 0.0,
        earth: Double = 0.0,
        organic: Double = 0.0,
        quantum: Double = 0.0,
        nether: Double = 0.0,
        precious: Double = 0.0,
        ender: Double = 0.0,
        living: Double = 0.0,
    ): List<MatterEntry> =
        buildList {
            addIfPositive("metallic", metallic)
            addIfPositive("earth", earth)
            addIfPositive("organic", organic)
            addIfPositive("quantum", quantum)
            addIfPositive("nether", nether)
            addIfPositive("precious", precious)
            addIfPositive("ender", ender)
            addIfPositive("living", living)
        }

    private fun MutableList<MatterEntry>.addIfPositive(type: String, value: Double) {
        if (value > 0.0) {
            add(MatterEntry(type, value))
        }
    }

    private data class MatterEntry(
        val type: String,
        val value: Double,
    )
}
