package com.p_nsk.replicated_integration.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.api.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterNodeKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.GsonHelper
import net.minecraft.util.profiling.ProfilerFiller

object MatterNodeValueReloadListener : SimpleJsonResourceReloadListener(Gson(), "replicated_integration/matter_node_values") {
    @Volatile
    private var values: Map<MatterNodeKey, ExplicitMatterValue> = emptyMap()

    fun snapshot(): Map<MatterNodeKey, ExplicitMatterValue> = values

    override fun apply(
        objects: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        val parsed = linkedMapOf<MatterNodeKey, ExplicitMatterValue>()

        for ((fileId, jsonElement) in objects) {
            try {
                val entry = parseEntry(fileId, GsonHelper.convertToJsonObject(jsonElement, "matter node value"))
                val previous = parsed[entry.node]
                parsed[entry.node] =
                    if (previous == null || previous !is ExplicitMatterValue.Set || entry.compound.isBetterThan(previous.compound)) {
                        ExplicitMatterValue.Set(entry.compound, ExplicitMatterSource.NODE_VALUE)
                    } else {
                        previous
                    }
            } catch (t: Throwable) {
                throw JsonParseException("Failed to parse matter node value $fileId", t)
            }
        }

        values = parsed.toMap()
        Constants.LOGGER.info("Loaded {} replicated_integration matter node value definitions", values.size)
    }

    private fun parseEntry(fileId: ResourceLocation, json: JsonObject): ParsedEntry {
        val type = parseResourceLocation(GsonHelper.getAsString(json, "type"), "type", fileId)
        val id = parseResourceLocation(GsonHelper.getAsString(json, "id"), "id", fileId)
        val matterValues = GsonHelper.getAsJsonArray(json, "matter")
        val values = linkedMapOf<LiteResourceLocation, Double>()

        for ((index, element) in matterValues.withIndex()) {
            val matterJson = GsonHelper.convertToJsonObject(element, "matter[$index]")
            val matterId = parseResourceLocation(GsonHelper.getAsString(matterJson, "type"), "matter[$index].type", fileId)
            val amount = GsonHelper.getAsDouble(matterJson, "value")
            require(amount > 0.0) {
                "matter[$index].value must be positive in $fileId"
            }
            values[LiteResourceLocation.of(matterId.namespace, matterId.path)] = amount
        }

        require(values.isNotEmpty()) { "matter must not be empty in $fileId" }
        return ParsedEntry(
            node = MatterNodeKey(
                LiteResourceLocation.of(type.namespace, type.path),
                LiteResourceLocation.of(id.namespace, id.path),
            ),
            compound = LiteMatterCompound(values),
        )
    }

    private fun parseResourceLocation(raw: String, field: String, fileId: ResourceLocation): ResourceLocation {
        return ResourceLocation.tryParse(raw)
            ?: throw JsonParseException("Invalid resource location '$raw' for $field in $fileId")
    }

    private data class ParsedEntry(
        val node: MatterNodeKey,
        val compound: LiteMatterCompound,
    )
}
