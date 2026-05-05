package com.p_nsk.replicated_integration.adapter.vanilla

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.api.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.IConversionSink
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.LiteResourceLocation
import com.p_nsk.replicated_integration.api.MatterAmount
import com.p_nsk.replicated_integration.api.MatterNodes
import com.p_nsk.replicated_integration.api.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.MatterValueRecipeExtension
import com.p_nsk.replicated_integration.api.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.ReplicationAddon
import com.p_nsk.replicated_integration.api.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.bridge.NeoRecipeConversionSupport
import com.p_nsk.replicated_integration.bridge.NeoReplicationAddonContext
import com.p_nsk.replicated_integration.data.MatterNodeValueReloadListener
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.BlastingRecipe
import net.minecraft.world.item.crafting.CampfireCookingRecipe
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SmokingRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe

object ReplicationVanillaAddon : ReplicationAddon<NeoReplicationAddonContext> {
    override val id: String = "vanilla"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    override fun collectDefaults(context: NeoReplicationAddonContext, defaults: MutableMatterDefaults) {
        importDefaults(context.defaultMatterRecipes, defaults)
        defaults.putAll(MatterNodeValueReloadListener.snapshot())
    }

    override fun collectConversions(context: NeoReplicationAddonContext, collector: IConversionSink) {
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
        recipes: Collection<RecipeHolder<MatterValueRecipe>>,
        defaults: MutableMatterDefaults,
    ) {
        for (holder in recipes) {
            for (stack in holder.value.input.items) {
                val node = BuiltinNodeResolver.itemNode(stack) ?: continue
                if ((holder.value as MatterValueRecipeExtension).replicatedIntegrationIsDenied()) {
                    defaults.deny(node, ExplicitMatterSource.DATAPACK)
                    continue
                }
                val compound = holder.value.matter.toLiteMatterCompound() ?: continue
                defaults.put(node, compound, ExplicitMatterSource.DATAPACK)
            }
        }
    }

    private fun Ingredient.toAlternativeMatterAmounts(): List<MatterAmount> =
        NeoRecipeConversionSupport.ingredientToAlternativeMatterAmounts(this, BuiltinNodeResolver::itemNode)

    private fun ItemStack.toItemMatterAmount(): MatterAmount? =
        BuiltinNodeResolver.itemAmount(this)

    private fun List<MatterValue>.toLiteMatterCompound(): LiteMatterCompound? {
        if (isEmpty()) {
            return null
        }
        val registry = ReplicationRegistry.MATTER_TYPES_REGISTRY ?: return null
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

    private fun createMappers(context: NeoReplicationAddonContext): List<RecipeConversionMapper<RecipeHolder<*>>> =
        listOf(
            singleOutputMapper<CraftingRecipe>(context) { holder ->
                val output = holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    holder.value.ingredients
                        .filterNot(Ingredient::isEmpty)
                        .map { it.toAlternativeMatterAmounts() },
                    output,
                    ::craftingCredits,
                )
            },
            singleOutputMapper<SmeltingRecipe>(context) { holder ->
                val output = holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(holder.id, listOf(holder.value.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), output)
            },
            singleOutputMapper<BlastingRecipe>(context) { holder ->
                val output = holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(holder.id, listOf(holder.value.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), output)
            },
            singleOutputMapper<SmokingRecipe>(context) { holder ->
                val output = holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(holder.id, listOf(holder.value.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), output)
            },
            singleOutputMapper<CampfireCookingRecipe>(context) { holder ->
                val output = holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(holder.id, listOf(holder.value.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), output)
            },
            singleOutputMapper<StonecutterRecipe>(context) { holder ->
                val output = holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(holder.id, listOf(holder.value.ingredients.firstOrNull()?.toAlternativeMatterAmounts().orEmpty()), output)
            },
        )

    private inline fun <reified R : Recipe<*>> singleOutputMapper(
        context: NeoReplicationAddonContext,
        crossinline extractor: NeoReplicationAddonContext.(RecipeHolder<R>) -> ConversionInputs?,
    ): RecipeConversionMapper<RecipeHolder<*>> =
        object : RecipeConversionMapper<RecipeHolder<*>> {
            override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is R

            @Suppress("UNCHECKED_CAST")
            override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                val inputs = context.extractor(recipe as RecipeHolder<R>) ?: return
                NeoRecipeConversionSupport.addConversionsForAlternatives(
                    id = inputs.id,
                    consumeAlternatives = inputs.consumeAlternatives,
                    produces = inputs.produces,
                    creditsOf = inputs.creditsOf,
                    collector = collector,
                )
            }
        }

    private fun craftingCredits(consumes: List<MatterAmount>): List<MatterAmount> =
        consumes
            .mapNotNull { consume ->
                if (consume.node.type != MatterNodes.ITEM) {
                    return@mapNotNull null
                }
                val item = itemById(consume.node.id) ?: return@mapNotNull null
                val remainder = item.craftingRemainingItem ?: return@mapNotNull null
                val remainderNode = BuiltinNodeResolver.itemNode(ItemStack(remainder)) ?: return@mapNotNull null
                MatterAmount(remainderNode, consume.amount)
            }
            .groupBy { it.node }
            .map { (node, amounts) -> MatterAmount(node, amounts.sumOf { it.amount }) }
            .sortedBy { it.node }

    private fun itemById(id: LiteResourceLocation): Item? {
        val key = ResourceLocation.fromNamespaceAndPath(id.namespace, id.path)
        val item = BuiltInRegistries.ITEM.get(key)
        return item.takeUnless { it == net.minecraft.world.item.Items.AIR }
    }

    private fun ResourceLocation.toLite(): LiteResourceLocation =
        NeoRecipeConversionSupport.run { toLite() }

    private data class ConversionInputs(
        val id: ResourceLocation,
        val consumeAlternatives: List<List<MatterAmount>>,
        val produces: MatterAmount,
        val creditsOf: (List<MatterAmount>) -> List<MatterAmount> = { emptyList() },
    )
}
