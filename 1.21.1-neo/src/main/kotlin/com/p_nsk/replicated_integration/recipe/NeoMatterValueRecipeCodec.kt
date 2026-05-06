package com.p_nsk.replicated_integration.recipe

import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.crafting.Ingredient
import java.util.Optional

object NeoMatterValueRecipeCodec {
    private data class RawRecipe(
        val input: Ingredient,
        val matter: Optional<List<MatterValue>>,
        val deny: Boolean,
    )

    val CODEC: MapCodec<MatterValueRecipe> =
        RecordCodecBuilder.mapCodec<RawRecipe> { instance ->
            instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(RawRecipe::input),
                MatterValue.CODEC.listOf().optionalFieldOf("matter").forGetter(RawRecipe::matter),
                Codec.BOOL.optionalFieldOf("deny", false).forGetter(RawRecipe::deny),
            ).apply(instance, ::RawRecipe)
        }.flatXmap(::decode, ::encode)

    private fun decode(raw: RawRecipe): DataResult<MatterValueRecipe> {
        if (raw.deny && raw.matter.isPresent) {
            return DataResult.error { "matter and deny=true cannot be specified together" }
        }
        if (!raw.deny && raw.matter.isEmpty) {
            return DataResult.error { "matter is required when deny is false" }
        }
        val matter = raw.matter.orElse(emptyList())
        if (!raw.deny && matter.isEmpty()) {
            return DataResult.error { "matter must not be empty" }
        }
        val recipe = MatterValueRecipe(raw.input, matter)
        recipe.replicatedIntegrationDenied = raw.deny
        return DataResult.success(recipe)
    }

    private fun encode(recipe: MatterValueRecipe): DataResult<RawRecipe> {
        val denied = recipe.replicatedIntegrationDenied
        val matter = if (denied) Optional.empty() else Optional.of(recipe.matter)
        if (!denied && recipe.matter.isEmpty()) {
            return DataResult.error { "matter must not be empty" }
        }
        return DataResult.success(RawRecipe(recipe.input, matter, denied))
    }
}
