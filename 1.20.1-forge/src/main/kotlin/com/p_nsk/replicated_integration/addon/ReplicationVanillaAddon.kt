package com.p_nsk.replicated_integration.addon

import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.api.IConversionSink
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterAmount
import com.p_nsk.replicated_integration.api.MatterNodeKey
import com.p_nsk.replicated_integration.api.MatterNodes
import com.p_nsk.replicated_integration.api.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.ReplicationAddon
import com.p_nsk.replicated_integration.api.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.data.MatterNodeValueReloadListener
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe

object ReplicationVanillaAddon : ReplicationAddon<ForgeReplicationAddonContext> {
    override val id: String = "vanilla"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    override fun collectDefaults(context: ForgeReplicationAddonContext, defaults: MutableMatterDefaults) {
        importDefaults(context.defaultMatterRecipes, defaults)
        defaults.putAll(MatterNodeValueReloadListener.snapshot())
    }

    override fun collectConversions(context: ForgeReplicationAddonContext, collector: IConversionSink) {
        val recipes = buildList {
            addAll(context.recipeManager.getAllRecipesFor(RecipeType.CRAFTING))
            addAll(context.recipeManager.getAllRecipesFor(RecipeType.SMELTING))
            addAll(context.recipeManager.getAllRecipesFor(RecipeType.BLASTING))
            addAll(context.recipeManager.getAllRecipesFor(RecipeType.SMOKING))
            addAll(context.recipeManager.getAllRecipesFor(RecipeType.CAMPFIRE_COOKING))
            addAll(context.recipeManager.getAllRecipesFor(RecipeType.STONECUTTING))
        }
        val mappers = createMappers(context)

        for (recipe in recipes) {
            mappers.firstOrNull { it.supports(recipe) }?.collect(recipe, collector)
        }
    }

    private fun importDefaults(
        recipes: List<MatterValueRecipe>,
        defaults: MutableMatterDefaults,
    ) {
        for (recipe in recipes) {
            val compound = recipe.matter.toLiteMatterCompound() ?: continue
            for (stack in recipe.input.items) {
                val node = stack.toItemNode() ?: continue
                defaults.put(node, compound)
            }
        }
    }

    private fun Ingredient.toAlternativeMatterAmounts(): List<MatterAmount> =
        ForgeRecipeConversionSupport.ingredientToAlternativeMatterAmounts(this) { stack -> stack.toItemNode() }

    private fun ItemStack.toItemNode(): MatterNodeKey? {
        if (isEmpty) {
            return null
        }
        val id = BuiltInRegistries.ITEM.getKey(item)
        return MatterNodes.item(id.toLite())
    }

    private fun ItemStack.toItemMatterAmount(): MatterAmount? {
        val node = toItemNode() ?: return null
        return MatterAmount(node, count.coerceAtLeast(1).toLong())
    }

    private fun Array<MatterValue>.toLiteMatterCompound(): LiteMatterCompound? {
        if (isEmpty()) {
            return null
        }
        val registry = com.buuz135.replication.ReplicationRegistry.MATTER_TYPES_REGISTRY.get()
        val values = linkedMapOf<LiteResourceLocation, Double>()
        for (matterValue in this) {
            val id = registry.getKey(matterValue.matter) ?: continue
            if (matterValue.amount <= 0.0) {
                continue
            }
            values[id.toLite()] = matterValue.amount
        }
        return if (values.isEmpty()) null else LiteMatterCompound(values)
    }

    private fun createMappers(context: ForgeReplicationAddonContext): List<RecipeConversionMapper<Recipe<*>>> =
        listOf(
            singleOutputMapper<CraftingRecipe>(context) { recipe ->
                val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    recipe.id,
                    recipe.ingredients
                        .filterNot(Ingredient::isEmpty)
                        .map { it.toAlternativeMatterAmounts() },
                    listOf(output),
                )
            },
            singleOutputMapper<SmeltingRecipe>(context) { recipe ->
                val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(recipe.id, listOf(recipe.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), listOf(output))
            },
            singleOutputMapper<BlastingRecipe>(context) { recipe ->
                val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(recipe.id, listOf(recipe.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), listOf(output))
            },
            singleOutputMapper<SmokingRecipe>(context) { recipe ->
                val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(recipe.id, listOf(recipe.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), listOf(output))
            },
            singleOutputMapper<CampfireCookingRecipe>(context) { recipe ->
                val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(recipe.id, listOf(recipe.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), listOf(output))
            },
            singleOutputMapper<StonecutterRecipe>(context) { recipe ->
                val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(recipe.id, listOf(recipe.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), listOf(output))
            },
        )

    private inline fun <reified R : Recipe<*>> singleOutputMapper(
        context: ForgeReplicationAddonContext,
        crossinline extractor: ForgeReplicationAddonContext.(R) -> ConversionInputs?,
    ): RecipeConversionMapper<Recipe<*>> =
        object : RecipeConversionMapper<Recipe<*>> {
            override fun supports(recipe: Any): Boolean = recipe is Recipe<*> && recipe is R

            @Suppress("UNCHECKED_CAST")
            override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
                val inputs = context.extractor(recipe as R) ?: return
                ForgeRecipeConversionSupport.addConversionsForAlternatives(
                    id = inputs.id,
                    consumeAlternatives = inputs.consumeAlternatives,
                    produces = inputs.produces,
                    collector = collector,
                )
            }
        }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        ForgeRecipeConversionSupport.run { toLite() }

    private data class ConversionInputs(
        val id: ResourceLocation,
        val consumeAlternatives: List<List<MatterAmount>>,
        val produces: List<MatterAmount>,
    )
}
