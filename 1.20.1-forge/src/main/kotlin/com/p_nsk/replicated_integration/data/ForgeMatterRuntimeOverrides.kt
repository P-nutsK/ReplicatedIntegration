package com.p_nsk.replicated_integration.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.p_nsk.replicated_integration.api.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterNodeKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path

object ForgeMatterRuntimeOverrides {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var loadedFrom: Path? = null
    private var values: Map<MatterNodeKey, ExplicitMatterValue> = emptyMap()

    @Synchronized
    fun snapshot(server: MinecraftServer): Map<MatterNodeKey, ExplicitMatterValue> {
        loadIfNeeded(server)
        return values
    }

    @Synchronized
    fun set(
        server: MinecraftServer,
        node: MatterNodeKey,
        compound: LiteMatterCompound,
    ) {
        loadIfNeeded(server)
        values = values.toMutableMap().also { it[node] = ExplicitMatterValue.Set(compound, ExplicitMatterSource.RUNTIME) }
        save(server)
    }

    @Synchronized
    fun deny(server: MinecraftServer, node: MatterNodeKey) {
        loadIfNeeded(server)
        values = values.toMutableMap().also { it[node] = ExplicitMatterValue.Deny(ExplicitMatterSource.RUNTIME) }
        save(server)
    }

    @Synchronized
    fun reset(server: MinecraftServer, node: MatterNodeKey) {
        loadIfNeeded(server)
        values = values.toMutableMap().also { it.remove(node) }
        save(server)
    }

    private fun loadIfNeeded(server: MinecraftServer) {
        val path = filePath(server)
        if (loadedFrom == path) {
            return
        }
        loadedFrom = path
        values = load(path)
    }

    private fun load(path: Path): Map<MatterNodeKey, ExplicitMatterValue> {
        if (!Files.exists(path)) {
            return emptyMap()
        }
        val root = JsonParser.parseString(Files.readString(path)).asJsonObject
        val entries = root.getAsJsonArray("entries") ?: JsonArray()
        val parsed = linkedMapOf<MatterNodeKey, ExplicitMatterValue>()
        for (element in entries) {
            val json = element.asJsonObject
            val node = MatterNodeKey(
                json.get("type").asString.toLite(),
                json.get("id").asString.toLite(),
            )
            val value =
                if (json.get("deny")?.asBoolean == true) {
                    ExplicitMatterValue.Deny(ExplicitMatterSource.RUNTIME)
                } else {
                    val matter = json.getAsJsonArray("matter")
                    val compound = linkedMapOf<LiteResourceLocation, Double>()
                    for (matterElement in matter) {
                        val matterJson = matterElement.asJsonObject
                        compound[matterJson.get("type").asString.toLite()] = matterJson.get("value").asDouble
                    }
                    ExplicitMatterValue.Set(LiteMatterCompound(compound), ExplicitMatterSource.RUNTIME)
                }
            parsed[node] = value
        }
        return parsed
    }

    private fun save(server: MinecraftServer) {
        val path = filePath(server)
        Files.createDirectories(path.parent)
        val root = JsonObject()
        val entries = JsonArray()
        for ((node, value) in values.entries.sortedBy { it.key }) {
            val json = JsonObject()
            json.addProperty("type", node.type.toString())
            json.addProperty("id", node.id.toString())
            when (value) {
                is ExplicitMatterValue.Deny -> json.addProperty("deny", true)
                is ExplicitMatterValue.Set -> {
                    val matter = JsonArray()
                    for ((matterId, amount) in value.compound.values.entries.sortedBy { it.key.toString() }) {
                        val matterJson = JsonObject()
                        matterJson.addProperty("type", matterId.toString())
                        matterJson.addProperty("value", amount)
                        matter.add(matterJson)
                    }
                    json.add("matter", matter)
                }
            }
            entries.add(json)
        }
        root.add("entries", entries)
        Files.writeString(path, gson.toJson(root))
    }

    private fun filePath(server: MinecraftServer): Path =
        server.getWorldPath(LevelResource.ROOT).resolve("data/replicated_integration/matter_overrides.json")

    private fun String.toLite(): LiteResourceLocation {
        val location = ResourceLocation.tryParse(this)
            ?: throw IllegalArgumentException("Invalid resource location: $this")
        return LiteResourceLocation.of(location.namespace, location.path)
    }
}
