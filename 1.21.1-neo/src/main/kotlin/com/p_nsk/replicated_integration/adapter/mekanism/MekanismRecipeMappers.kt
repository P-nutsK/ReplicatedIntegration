package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.MatterAmount
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.node.MatterNodeKey
import com.p_nsk.replicated_integration.bridge.NeoRecipeConversionSupport
import mekanism.api.chemical.ChemicalStack
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe
import mekanism.api.recipes.ChemicalCrystallizerRecipe
import mekanism.api.recipes.ChemicalDissolutionRecipe
import mekanism.api.recipes.ChemicalToChemicalRecipe
import mekanism.api.recipes.CombinerRecipe
import mekanism.api.recipes.ElectrolysisRecipe
import mekanism.api.recipes.FluidChemicalToChemicalRecipe
import mekanism.api.recipes.FluidToFluidRecipe
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe
import mekanism.api.recipes.ItemStackToChemicalRecipe
import mekanism.api.recipes.ItemStackToItemStackRecipe
import mekanism.api.recipes.MekanismRecipeTypes
import mekanism.api.recipes.PressurizedReactionRecipe
import mekanism.api.recipes.RotaryRecipe
import mekanism.api.recipes.ingredients.ChemicalStackIngredient
import mekanism.api.recipes.ingredients.FluidStackIngredient
import mekanism.api.recipes.ingredients.InputIngredient
import mekanism.api.recipes.ingredients.ItemStackIngredient
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.neoforged.neoforge.fluids.FluidStack

object MekanismRecipeMappers {
    private const val COMPRESSING_GAS_SCALE = 200L

    val all: List<RecipeConversionMapper<RecipeHolder<*>>> =
        listOf(
            singleOutput<ItemStackToItemStackRecipe> { holder ->
                build(holder.id, listOf(holder.value.input.toAlternativeMatterAmounts()), holder.value.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount))
            },
            singleOutput<ItemStackToChemicalRecipe> { holder ->
                build(holder.id, listOf(holder.value.input.toAlternativeMatterAmounts()), holder.value.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount))
            },
            singleOutput<ChemicalToChemicalRecipe> { holder ->
                build(holder.id, listOf(holder.value.input.toAlternativeMatterAmounts()), holder.value.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount))
            },
            singleOutput<ChemicalChemicalToChemicalRecipe> { holder ->
                build(
                    holder.id,
                    listOf(
                        holder.value.leftInput.toAlternativeMatterAmounts(),
                        holder.value.rightInput.toAlternativeMatterAmounts(),
                    ),
                    holder.value.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<ItemStackChemicalToItemStackRecipe> { holder ->
                build(
                    holder.id,
                    listOf(
                        holder.value.itemInput.toAlternativeMatterAmounts(),
                        holder.value.chemicalInput.toAlternativeMatterAmounts(holder.chemicalAmountScale()),
                    ),
                    holder.value.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount),
                )
            },
            singleOutput<FluidChemicalToChemicalRecipe> { holder ->
                build(
                    holder.id,
                    listOf(
                        holder.value.fluidInput.toAlternativeMatterAmounts(),
                        holder.value.chemicalInput.toAlternativeMatterAmounts(),
                    ),
                    holder.value.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<ChemicalCrystallizerRecipe> { holder ->
                build(holder.id, listOf(holder.value.input.toAlternativeMatterAmounts()), holder.value.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount))
            },
            singleOutput<ChemicalDissolutionRecipe> { holder ->
                build(
                    holder.id,
                    listOf(
                        holder.value.itemInput.toAlternativeMatterAmounts(),
                        holder.value.chemicalInput.toAlternativeMatterAmounts(),
                    ),
                    holder.value.outputDefinition.singleOrNull()?.let(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<CombinerRecipe> { holder ->
                build(
                    holder.id,
                    listOf(
                        holder.value.mainInput.toAlternativeMatterAmounts(),
                        holder.value.extraInput.toAlternativeMatterAmounts(),
                    ),
                    holder.value.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount),
                )
            },
            singleOutput<FluidToFluidRecipe> { holder ->
                build(holder.id, listOf(holder.value.input.toAlternativeMatterAmounts()), holder.value.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::fluidAmount))
            },
            object : RecipeConversionMapper<RecipeHolder<*>> {
                override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is RotaryRecipe

                override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                    val holder = recipe as RecipeHolder<RotaryRecipe>
                    val rotary = holder.value
                    val loopGuardKey = holder.id.withSuffix("rotary_loop_guard").toLite()
                    if (rotary.hasFluidToChemical()) {
                        val output = rotary.chemicalOutputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount)
                        if (output != null) {
                            NeoRecipeConversionSupport.addConversionsForAlternatives(
                                id = holder.id.withSuffix("fluid_to_chemical"),
                                consumeAlternatives = listOf(rotary.fluidInput.toAlternativeMatterAmounts()),
                                produces = output,
                                loopGuardKey = loopGuardKey,
                                collector = collector,
                            )
                        }
                    }
                    if (rotary.hasChemicalToFluid()) {
                        val output = rotary.fluidOutputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::fluidAmount)
                        if (output != null) {
                            NeoRecipeConversionSupport.addConversionsForAlternatives(
                                id = holder.id.withSuffix("chemical_to_fluid"),
                                consumeAlternatives = listOf(rotary.chemicalInput.toAlternativeMatterAmounts()),
                                produces = output,
                                loopGuardKey = loopGuardKey,
                                collector = collector,
                            )
                        }
                    }
                }
            },
            object : RecipeConversionMapper<RecipeHolder<*>> {
                override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is ElectrolysisRecipe

                override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                    val holder = recipe as RecipeHolder<ElectrolysisRecipe>
                    val output = holder.value.outputDefinition.singleOrNull() ?: return
                    val consumeAlternatives = listOf(holder.value.input.toAlternativeMatterAmounts())
                    val left = MekanismNodeResolver.chemicalAmount(output.left)
                    val right = MekanismNodeResolver.chemicalAmount(output.right)
                    if (left != null) {
                        NeoRecipeConversionSupport.addConversionsForAlternatives(
                            id = holder.id.withSuffix("left"),
                            consumeAlternatives = consumeAlternatives,
                            produces = left,
                            creditsOf = { listOfNotNull(right) },
                            collector = collector,
                        )
                    }
                    if (right != null) {
                        NeoRecipeConversionSupport.addConversionsForAlternatives(
                            id = holder.id.withSuffix("right"),
                            consumeAlternatives = consumeAlternatives,
                            produces = right,
                            creditsOf = { listOfNotNull(left) },
                            collector = collector,
                        )
                    }
                }
            },
            object : RecipeConversionMapper<RecipeHolder<*>> {
                override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is PressurizedReactionRecipe

                override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                    val holder = recipe as RecipeHolder<PressurizedReactionRecipe>
                    val output = holder.value.outputDefinition.singleOrNull() ?: return
                    val consumeAlternatives =
                        listOf(
                            holder.value.inputSolid.toAlternativeMatterAmounts(),
                            holder.value.inputFluid.toAlternativeMatterAmounts(),
                            holder.value.inputChemical.toAlternativeMatterAmounts(),
                        )
                    val item = BuiltinNodeResolver.itemAmount(output.item)
                    val chemical = MekanismNodeResolver.chemicalAmount(output.chemical)
                    if (item != null) {
                        NeoRecipeConversionSupport.addConversionsForAlternatives(
                            id = holder.id.withSuffix("item"),
                            consumeAlternatives = consumeAlternatives,
                            produces = item,
                            creditsOf = { listOfNotNull(chemical) },
                            collector = collector,
                        )
                    }
                    if (chemical != null) {
                        NeoRecipeConversionSupport.addConversionsForAlternatives(
                            id = holder.id.withSuffix("chemical"),
                            consumeAlternatives = consumeAlternatives,
                            produces = chemical,
                            creditsOf = { listOfNotNull(item) },
                            collector = collector,
                        )
                    }
                }
            },
        )

    private fun build(
        id: ResourceLocation,
        consumeAlternatives: List<List<MatterAmount>>,
        produces: MatterAmount?,
    ): ConversionInputs? =
        produces?.let { ConversionInputs(id, consumeAlternatives, it) }

    private inline fun <reified R : Recipe<*>> singleOutput(
        crossinline extractor: (RecipeHolder<R>) -> ConversionInputs?,
    ): RecipeConversionMapper<RecipeHolder<*>> =
        object : RecipeConversionMapper<RecipeHolder<*>> {
            override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is R

            @Suppress("UNCHECKED_CAST")
            override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                val inputs = extractor(recipe as RecipeHolder<R>) ?: return
                NeoRecipeConversionSupport.addConversionsForAlternatives(
                    id = inputs.id,
                    consumeAlternatives = inputs.consumeAlternatives,
                    produces = inputs.produces,
                    creditsOf = inputs.creditsOf,
                    collector = collector,
                )
            }
        }

    private fun ItemStackIngredient.toAlternativeMatterAmounts(): List<MatterAmount> =
        ingredientToAlternativeMatterAmounts(
            ingredient = this as InputIngredient<ItemStack>,
            nodeOf = BuiltinNodeResolver::itemNode,
        )

    private fun FluidStackIngredient.toAlternativeMatterAmounts(): List<MatterAmount> =
        ingredientToAlternativeMatterAmounts(
            ingredient = this as InputIngredient<FluidStack>,
            nodeOf = BuiltinNodeResolver::fluidNode,
        )

    @Suppress("UNCHECKED_CAST")
    private fun ChemicalStackIngredient.toAlternativeMatterAmounts(scale: Long = 1L): List<MatterAmount> =
        ingredientToAlternativeMatterAmounts(
            ingredient = this as InputIngredient<ChemicalStack>,
            nodeOf = MekanismNodeResolver::chemicalNode,
            scale = scale,
        )

    private fun <T> ingredientToAlternativeMatterAmounts(
        ingredient: InputIngredient<T>,
        nodeOf: (T) -> MatterNodeKey?,
        scale: Long = 1L,
    ): List<MatterAmount> =
        ingredient.representations.mapNotNull { representation ->
            val node = nodeOf(representation) ?: return@mapNotNull null
            val needed = ingredient.getNeededAmount(representation) * scale
            if (needed <= 0) {
                null
            } else {
                MatterAmount(node, needed)
            }
        }

    private fun RecipeHolder<ItemStackChemicalToItemStackRecipe>.chemicalAmountScale(): Long =
        if (value.type == MekanismRecipeTypes.TYPE_COMPRESSING.value()) {
            COMPRESSING_GAS_SCALE
        } else {
            1L
        }

    private fun <T> List<T>.singleMatterAmountOrNull(mapper: (T) -> MatterAmount?): MatterAmount? =
        singleOrNull()?.let(mapper)

    private fun ResourceLocation.withSuffix(suffix: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, "$path/$suffix")

    private fun ResourceLocation.toLite() =
        NeoRecipeConversionSupport.run { toLite() }

    private data class ConversionInputs(
        val id: ResourceLocation,
        val consumeAlternatives: List<List<MatterAmount>>,
        val produces: MatterAmount,
        val creditsOf: (List<MatterAmount>) -> List<MatterAmount> = { emptyList() },
    )
}
