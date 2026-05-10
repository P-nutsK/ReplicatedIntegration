package com.p_nsk.replicated_integration.api.selector

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.p_nsk.replicated_integration.api.model.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import java.nio.file.Files
import java.nio.file.Path

object MatterSelectorStorageCodec {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun load(
        path: Path,
        source: ExplicitMatterSource,
    ): Map<MatterSelectorKey, ExplicitMatterValue> {
        if (!Files.exists(path)) {
            return emptyMap()
        }
        val root = JsonParser.parseString(Files.readString(path)).asJsonObject
        val entries = root.getAsJsonArray("entries") ?: JsonArray()
        val parsed = linkedMapOf<MatterSelectorKey, ExplicitMatterValue>()
        for (element in entries) {
            val json = element.asJsonObject
            val selector =
                MatterSelectorKey(
                    kind =
                        when (json["selector"]?.asString ?: "node") {
                            "node" -> MatterSelectorKind.NODE
                            "tag" -> MatterSelectorKind.TAG
                            else -> throw IllegalArgumentException("Unknown selector kind: ${json["selector"]}")
                        },
                    type = json["type"].asString.toLite(),
                    id = json["id"].asString.toLite(),
                )
            val value =
                if (json["deny"]?.asBoolean == true) {
                    ExplicitMatterValue.Deny(source)
                } else {
                    val matter = json.getAsJsonArray("matter")
                    val compound = linkedMapOf<LiteResourceLocation, Double>()
                    for (matterElement in matter) {
                        val matterJson = matterElement.asJsonObject
                        compound[matterJson["type"].asString.toLite()] = matterJson["value"].asDouble
                    }
                    ExplicitMatterValue.Set(LiteMatterCompound(compound), source)
                }
            parsed[selector] = value
        }
        return parsed
    }

    fun save(
        path: Path,
        values: Map<MatterSelectorKey, ExplicitMatterValue>,
    ) {
        Files.createDirectories(path.parent)
        val root = JsonObject()
        val entries = JsonArray()
        for ((selector, value) in values.entries.sortedBy { it.key }) {
            val json = JsonObject()
            json.addProperty("selector", selector.kind.name.lowercase())
            json.addProperty("type", selector.type.toString())
            json.addProperty("id", selector.id.toString())
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

    private fun String.toLite(): LiteResourceLocation {
        val parts = split(':', limit = 2)
        require(parts.size == 2) { "Invalid resource location: $this" }
        return LiteResourceLocation.of(parts[0], parts[1])
    }
}
