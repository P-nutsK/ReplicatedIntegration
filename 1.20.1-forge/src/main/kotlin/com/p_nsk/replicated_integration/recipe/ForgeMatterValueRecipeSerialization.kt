package com.p_nsk.replicated_integration.recipe

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.p_nsk.replicated_integration.api.recipe.MatterValueRecipeExtension
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient

object ForgeMatterValueRecipeSerialization {
    fun fromJson(
        id: ResourceLocation,
        json: JsonObject,
    ): MatterValueRecipe {
        val input = Ingredient.fromJson(json.get("input"))
        val deny = json.get("deny")?.asBoolean ?: false
        val hasMatter = json.has("matter")
        require(!(deny && hasMatter)) { "matter and deny=true cannot be specified together in $id" }
        require(deny || hasMatter) { "matter is required when deny is false in $id" }
        val matter =
            if (hasMatter) {
                readMatterValues(json.getAsJsonArray("matter"), id)
            } else {
                emptyArray()
            }
        require(deny || matter.isNotEmpty()) { "matter must not be empty in $id" }
        return MatterValueRecipe(id, input, *matter).also {
            (it as MatterValueRecipeExtension).`replicated_integration$setDenied`(deny)
        }
    }

    fun write(recipe: MatterValueRecipe, recipeTypeId: String): JsonObject {
        val json = JsonObject()
        json.addProperty("type", recipeTypeId)
        json.add("input", recipe.input.toJson())
        if (recipe.replicatedIntegrationDenied) {
            json.addProperty("deny", true)
        } else {
            val matter = JsonArray()
            for (value in recipe.matter) {
                val matterJson = JsonObject()
                val typeId = ReplicationRegistry.MATTER_TYPES_REGISTRY.get().getKey(value.matter)
                    ?: error("Unknown matter type ${value.matter}")
                matterJson.addProperty("type", typeId.toString())
                matterJson.addProperty("value", value.amount)
                matter.add(matterJson)
            }
            json.add("matter", matter)
        }
        return json
    }

    fun toNetwork(
        buf: FriendlyByteBuf,
        recipe: MatterValueRecipe,
    ) {
        recipe.input.toNetwork(buf)
        val denied = recipe.replicatedIntegrationDenied
        buf.writeBoolean(denied)
        if (!denied) {
            buf.writeVarInt(recipe.matter.size)
            for (value in recipe.matter) {
                buf.writeNbt(value.serializeNBT())
            }
        }
    }

    fun fromNetwork(
        id: ResourceLocation,
        buf: FriendlyByteBuf,
    ): MatterValueRecipe {
        val input = Ingredient.fromNetwork(buf)
        val denied = buf.readBoolean()
        val matter =
            if (denied) {
                emptyArray()
            } else {
                Array(buf.readVarInt()) {
                    MatterValue(ReplicationRegistry.Matter.EMPTY.get(), 0.0).also { value ->
                        value.deserializeNBT(buf.readNbt() ?: error("Missing matter value payload"))
                    }
                }
            }
        return MatterValueRecipe(id, input, *matter).also {
            (it as MatterValueRecipeExtension).`replicated_integration$setDenied`(denied)
        }
    }

    private fun readMatterValues(
        array: JsonArray,
        id: ResourceLocation,
    ): Array<MatterValue> {
        val registry = ReplicationRegistry.MATTER_TYPES_REGISTRY.get()
        return Array(array.size()) { index ->
            val json = array[index].asJsonObject
            val typeId = ResourceLocation.parse(json.get("type").asString)
            val type = registry.getValue(typeId)
                ?: error("Unknown matter type $typeId in $id")
            MatterValue(type, json.get("value").asDouble)
        }
    }
}
