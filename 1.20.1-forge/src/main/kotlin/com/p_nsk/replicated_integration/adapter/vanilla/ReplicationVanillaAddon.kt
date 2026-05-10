package com.p_nsk.replicated_integration.adapter.vanilla

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.model.*
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.node.MatterNodes.ITEM
import com.p_nsk.replicated_integration.api.node.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import com.p_nsk.replicated_integration.api.selector.MutableMatterSelectors
import com.p_nsk.replicated_integration.core.*
import com.p_nsk.replicated_integration.data.MatterNodeValueReloadListener
import com.p_nsk.replicated_integration.mixin.IngredientAccessor
import com.p_nsk.replicated_integration.recipe.replicatedIntegrationDenied
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.*

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ReplicationVanillaAddon : ForgeReplicationAddon {
    override val id: String = "vanilla"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    override fun collectDefaults(context: ForgeReplicationAddonContext, defaults: MutableMatterDefaults) {
        defaults.putAll(MatterNodeValueReloadListener.snapshot())
    }

    override fun collectSelectors(context: ForgeReplicationAddonContext, selectors: MutableMatterSelectors) {
        importDefaults(context.defaultMatterRecipes, selectors)
    }

    override fun registerMatterNodes(registry: ForgeMatterNodeRegistry) = with(registry) {
        node(MatterNodes.ITEM, "Item") {
            value(
                literal = "item",
                suggestions = VanillaMatterCommandInputs.itemSuggestions(),
                validate = VanillaMatterCommandInputs.itemValidator(),
            )

            tag(
                literal = "item_tag",
                suggestions = VanillaMatterCommandInputs.itemTagSuggestions(),
                validate = VanillaMatterCommandInputs.itemTagValidator(),
            )
        }

        node(MatterNodes.FLUID, "Fluid") {
            value(
                literal = "fluid",
                suggestions = VanillaMatterCommandInputs.fluidSuggestions(),
                validate = VanillaMatterCommandInputs.fluidValidator(),
            )

            tag(
                literal = "fluid_tag",
                suggestions = VanillaMatterCommandInputs.fluidTagSuggestions(),
                validate = VanillaMatterCommandInputs.fluidTagValidator(),
            )
        }
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
        selectors: MutableMatterSelectors,
    ) {
        for (recipe in recipes) {
            val explicitSelector = recipe.input.explicitDefaultSelectorOrNull()
            if (explicitSelector != null) {
                if (recipe.replicatedIntegrationDenied) {
                    selectors.deny(explicitSelector, ExplicitMatterSource.DATAPACK)
                    continue
                }
                val compound = recipe.matter.toLiteMatterCompound() ?: continue
                selectors.put(explicitSelector, compound, ExplicitMatterSource.DATAPACK)
                continue
            }
            for (stack in recipe.input.items) {
                val itemNode = BuiltinNodeResolver.itemNode(stack) ?: continue
                if (recipe.replicatedIntegrationDenied) {
                    selectors.deny(
                        MatterSelectorKey(MatterSelectorKind.NODE, itemNode.type, itemNode.id),
                        ExplicitMatterSource.DATAPACK
                    )
                    continue
                }
                val compound = recipe.matter.toLiteMatterCompound() ?: continue
                selectors.put(
                    MatterSelectorKey(MatterSelectorKind.NODE, itemNode.type, itemNode.id),
                    compound,
                    ExplicitMatterSource.DATAPACK
                )
            }
        }
    }

    private fun Ingredient.toInputNode(): InputNodes =
        ForgeRecipeConversionSupport.ingredientToInputNode(this, BuiltinNodeResolver::itemNode)

    private fun ItemStack.toItemMatterAmount(): NodeAmount? = BuiltinNodeResolver.itemAmount(this)

    private fun Array<MatterValue>.toLiteMatterCompound(): LiteMatterCompound? {
        if (isEmpty()) {
            return null
        }
        val registry = ReplicationRegistry.MATTER_TYPES_REGISTRY.get()
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

    private fun createMappers(context: ForgeReplicationAddonContext): List<RecipeConversionMapper<Recipe<*>>> = listOf(
        singleOutputMapper<CraftingRecipe>(context) { recipe ->
            val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
            ConversionInputs(
                recipe.id,
                recipe.ingredients.filterNot(Ingredient::isEmpty).map { it.toInputNode() },
                output,
                ::craftingCredits,
            )
        },
        singleOutputMapper<SmeltingRecipe>(context) { recipe ->
            val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
            ConversionInputs(recipe.id, listOf(recipe.ingredients.firstInputNode()), output)
        },
        singleOutputMapper<BlastingRecipe>(context) { recipe ->
            val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
            ConversionInputs(recipe.id, listOf(recipe.ingredients.firstInputNode()), output)
        },
        singleOutputMapper<SmokingRecipe>(context) { recipe ->
            val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
            ConversionInputs(recipe.id, listOf(recipe.ingredients.firstInputNode()), output)
        },
        singleOutputMapper<CampfireCookingRecipe>(context) { recipe ->
            val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
            ConversionInputs(recipe.id, listOf(recipe.ingredients.firstInputNode()), output)
        },
        singleOutputMapper<StonecutterRecipe>(context) { recipe ->
            val output = recipe.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
            ConversionInputs(recipe.id, listOf(recipe.ingredients.firstInputNode()), output)
        },
    )

    private fun Ingredient.explicitDefaultSelectorOrNull() =
        explicitDefaultTagSelectorOrNull() ?: explicitDefaultItemSelectorOrNull()

    private fun Ingredient.explicitDefaultItemSelectorOrNull() =
        ((this as IngredientAccessor).`replicated_integration$getValues`()
            .singleOrNull() as? Ingredient.ItemValue)?.serialize()?.getAsJsonPrimitive("item")?.asString?.let(
            ResourceLocation::parse
        )?.let { itemId ->
            MatterSelectorKey(
                MatterSelectorKind.NODE, ITEM, ForgeRecipeConversionSupport.run { itemId.toLite() })
        }

    private fun Ingredient.explicitDefaultTagSelectorOrNull() =
        ((this as IngredientAccessor).`replicated_integration$getValues`()
            .singleOrNull() as? Ingredient.TagValue)?.serialize()?.getAsJsonPrimitive("tag")?.asString?.let(
            ResourceLocation::parse
        )?.let { tagId ->
            MatterSelectorKey(
                MatterSelectorKind.TAG, ITEM, ForgeRecipeConversionSupport.run { tagId.toLite() })
        }

    private inline fun <reified R : Recipe<*>> singleOutputMapper(
        context: ForgeReplicationAddonContext,
        crossinline extractor: ForgeReplicationAddonContext.(R) -> ConversionInputs?,
    ): RecipeConversionMapper<Recipe<*>> = object : RecipeConversionMapper<Recipe<*>> {
        override fun supports(recipe: Any): Boolean = recipe is Recipe<*> && recipe is R

        @Suppress("UNCHECKED_CAST")
        override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
            val inputs = context.extractor(recipe as R) ?: return
            ForgeRecipeConversionSupport.addConversion(
                id = inputs.id,
                consumeInputNodes = inputs.consumeAlternatives,
                produces = inputs.produces,
                creditsOf = inputs.creditsOf,
                collector = collector,
            )
        }
    }

    private fun craftingCredits(consumes: List<NodeAmount>): List<NodeAmount> = consumes.mapNotNull { consume ->
        if (consume.node.type != ITEM) {
            return@mapNotNull null
        }
        val item = itemById(consume.node.id) ?: return@mapNotNull null
        val remainder = item.craftingRemainingItem ?: return@mapNotNull null
        val remainderNode = BuiltinNodeResolver.itemNode(ItemStack(remainder)) ?: return@mapNotNull null
        NodeAmount(remainderNode, consume.amount)
    }.groupBy { it.node }.map { (node, amounts) -> NodeAmount(node, amounts.sumOf { it.amount }) }
        .sortedBy { it.node }

    private fun itemById(id: LiteResourceLocation): Item? {
        val key = ResourceLocation.fromNamespaceAndPath(id.namespace, id.path)
        val item = BuiltInRegistries.ITEM.get(key)
        return item.takeUnless { it == Items.AIR }
    }

    private fun List<Ingredient>.firstInputNode(): InputNodes = firstOrNull()?.toInputNode() ?: InputNodes.empty()

    private fun ResourceLocation.toLite(): LiteResourceLocation = ForgeRecipeConversionSupport.run { toLite() }

    private data class ConversionInputs(
        val id: ResourceLocation,
        val consumeAlternatives: List<InputNodes>,
        val produces: NodeAmount,
        val creditsOf: (List<NodeAmount>) -> List<NodeAmount> = { emptyList() },
    )
}
