package com.p_nsk.replicated_integration.adapter.vanilla

import com.buuz135.replication.ReplicationRegistry
import com.buuz135.replication.calculation.MatterValue
import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.model.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.node.MatterNodes.FLUID
import com.p_nsk.replicated_integration.api.node.MatterNodes.ITEM
import com.p_nsk.replicated_integration.api.node.MatterNodes
import com.p_nsk.replicated_integration.api.node.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKind
import com.p_nsk.replicated_integration.api.selector.MutableMatterSelectors
import com.p_nsk.replicated_integration.core.NeoMatterNodeRegistry
import com.p_nsk.replicated_integration.core.NeoRecipeConversionSupport
import com.p_nsk.replicated_integration.core.NeoReplicationAddon
import com.p_nsk.replicated_integration.core.NeoReplicationAddonContext
import com.p_nsk.replicated_integration.core.node
import com.p_nsk.replicated_integration.data.MatterNodeValueReloadListener
import com.p_nsk.replicated_integration.recipe.replicatedIntegrationDenied
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.*

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ReplicationVanillaAddon : NeoReplicationAddon {
    override val id: String = "vanilla"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    override fun collectDefaults(context: NeoReplicationAddonContext, defaults: MutableMatterDefaults) {
        defaults.putAll(MatterNodeValueReloadListener.snapshot())
    }

    override fun collectSelectors(context: NeoReplicationAddonContext, selectors: MutableMatterSelectors) {
        importDefaults(context.defaultMatterRecipes, selectors)
    }

    override fun registerMatterNodes(registry: NeoMatterNodeRegistry) = with(registry) {
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
        selectors: MutableMatterSelectors,
    ) {
        for (holder in recipes) {
            val explicitSelector = holder.value.input.explicitDefaultSelectorOrNull()
            if (explicitSelector != null) {
                if (holder.value.replicatedIntegrationDenied) {
                    selectors.deny(explicitSelector, ExplicitMatterSource.DATAPACK)
                    continue
                }
                val compound = holder.value.matter.toLiteMatterCompound() ?: continue
                selectors.put(explicitSelector, compound, ExplicitMatterSource.DATAPACK)
                continue
            }
            for (stack in holder.value.input.items) {
                val itemNode = BuiltinNodeResolver.itemNode(stack) ?: continue
                if (holder.value.replicatedIntegrationDenied) {
                    selectors.deny(
                        MatterSelectorKey(MatterSelectorKind.NODE, itemNode.type, itemNode.id),
                        ExplicitMatterSource.DATAPACK
                    )
                    continue
                }
                val compound = holder.value.matter.toLiteMatterCompound() ?: continue
                selectors.put(
                    MatterSelectorKey(MatterSelectorKind.NODE, itemNode.type, itemNode.id),
                    compound,
                    ExplicitMatterSource.DATAPACK
                )
            }
        }
    }

    private fun Ingredient.toInputNode(): InputNodes =
        NeoRecipeConversionSupport.ingredientToInputNode(this, BuiltinNodeResolver::itemNode)

    private fun ItemStack.toItemMatterAmount(): NodeAmount? =
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
                val output =
                    holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    holder.value.ingredients
                        .filterNot(Ingredient::isEmpty)
                        .map { it.toInputNode() },
                    output,
                    ::craftingCredits,
                )
            },
            singleOutputMapper<SmeltingRecipe>(context) { holder ->
                val output =
                    holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    listOf(holder.value.ingredients.firstOrNull()?.toInputNode() ?: InputNodes.empty()),
                    output
                )
            },
            singleOutputMapper<BlastingRecipe>(context) { holder ->
                val output =
                    holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    listOf(holder.value.ingredients.firstOrNull()?.toInputNode() ?: InputNodes.empty()),
                    output
                )
            },
            singleOutputMapper<SmokingRecipe>(context) { holder ->
                val output =
                    holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    listOf(holder.value.ingredients.firstOrNull()?.toInputNode() ?: InputNodes.empty()),
                    output
                )
            },
            singleOutputMapper<CampfireCookingRecipe>(context) { holder ->
                val output =
                    holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    listOf(holder.value.ingredients.firstOrNull()?.toInputNode() ?: InputNodes.empty()),
                    output
                )
            },
            singleOutputMapper<StonecutterRecipe>(context) { holder ->
                val output =
                    holder.value.getResultItem(registryAccess).toItemMatterAmount() ?: return@singleOutputMapper null
                ConversionInputs(
                    holder.id,
                    listOf(holder.value.ingredients.firstOrNull()?.toInputNode() ?: InputNodes.empty()),
                    output
                )
            },
        )

    private fun Ingredient.explicitDefaultSelectorOrNull() =
        explicitDefaultTagSelectorOrNull() ?: explicitDefaultItemSelectorOrNull()

    private fun Ingredient.explicitDefaultItemSelectorOrNull() =
        if (isCustom || values.size != 1) null else (values.singleOrNull() as? Ingredient.ItemValue)?.item()
            ?.let { stack ->
                MatterSelectorKey(
                    MatterSelectorKind.NODE,
                    MatterNodes.ITEM,
                    NeoRecipeConversionSupport.run { BuiltInRegistries.ITEM.getKey(stack.item).toLite() })
            }

    private fun Ingredient.explicitDefaultTagSelectorOrNull() =
        if (isCustom || values.size != 1) null else (values.singleOrNull() as? Ingredient.TagValue)?.tag()?.location()
            ?.let { tagId ->
                MatterSelectorKey(
                    MatterSelectorKind.TAG,
                    MatterNodes.ITEM,
                    NeoRecipeConversionSupport.run { tagId.toLite() })
            }

    private inline fun <reified R : Recipe<*>> singleOutputMapper(
        context: NeoReplicationAddonContext,
        crossinline extractor: NeoReplicationAddonContext.(RecipeHolder<R>) -> ConversionInputs?,
    ): RecipeConversionMapper<RecipeHolder<*>> =
        object : RecipeConversionMapper<RecipeHolder<*>> {
            override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is R

            @Suppress("UNCHECKED_CAST")
            override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                val inputs = context.extractor(recipe as RecipeHolder<R>) ?: return
                NeoRecipeConversionSupport.addConversion(
                    id = inputs.id,
                    consumeInputNodes = inputs.consumeInputNodes,
                    produces = inputs.produces,
                    creditsOf = inputs.creditsOf,
                    collector = collector,
                )
            }
        }

    private fun craftingCredits(consumes: List<NodeAmount>): List<NodeAmount> =
        consumes
            .mapNotNull { consume ->
                if (consume.node.type != MatterNodes.ITEM) {
                    return@mapNotNull null
                }
                val item = itemById(consume.node.id) ?: return@mapNotNull null
                val remainder = item.craftingRemainingItem ?: return@mapNotNull null
                val remainderNode = BuiltinNodeResolver.itemNode(ItemStack(remainder)) ?: return@mapNotNull null
                NodeAmount(remainderNode, consume.amount)
            }
            .groupBy { it.node }
            .map { (node, amounts) -> NodeAmount(node, amounts.sumOf { it.amount }) }
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
        val consumeInputNodes: List<InputNodes>,
        val produces: NodeAmount,
        val creditsOf: (List<NodeAmount>) -> List<NodeAmount> = { emptyList() },
    )
}
