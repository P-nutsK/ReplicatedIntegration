package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.IConversionSink
import com.p_nsk.replicated_integration.api.MatterAmount
import com.p_nsk.replicated_integration.api.RecipeConversionMapper
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.bridge.ForgeRecipeConversionSupport
import mekanism.api.chemical.ChemicalStack
import mekanism.api.recipes.ChemicalCrystallizerRecipe
import mekanism.api.recipes.ChemicalDissolutionRecipe
import mekanism.api.recipes.CombinerRecipe
import mekanism.api.recipes.ElectrolysisRecipe
import mekanism.api.recipes.FluidToFluidRecipe
import mekanism.api.recipes.ItemStackGasToItemStackRecipe
import mekanism.api.recipes.ItemStackToItemStackRecipe
import mekanism.api.recipes.PressurizedReactionRecipe
import mekanism.api.recipes.RotaryRecipe
import mekanism.api.recipes.chemical.ChemicalChemicalToChemicalRecipe
import mekanism.api.recipes.chemical.ChemicalToChemicalRecipe
import mekanism.api.recipes.chemical.FluidChemicalToChemicalRecipe
import mekanism.api.recipes.chemical.ItemStackChemicalToItemStackRecipe
import mekanism.api.recipes.chemical.ItemStackToChemicalRecipe
import mekanism.api.recipes.ingredients.ChemicalStackIngredient
import mekanism.api.recipes.ingredients.FluidStackIngredient
import mekanism.api.recipes.ingredients.InputIngredient
import mekanism.api.recipes.ingredients.ItemStackIngredient
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.registries.ForgeRegistries

object MekanismRecipeMappers {
    private const val COMPRESSING_GAS_SCALE = 200L
    private val COMPRESSING_RECIPE_TYPE_ID = ResourceLocation.fromNamespaceAndPath("mekanism", "compressing")

    val all: List<RecipeConversionMapper<Recipe<*>>> =
        listOf(
            singleOutput<ItemStackToItemStackRecipe> { recipe ->
                build(recipe.id, listOf(recipe.input.toAlternativeMatterAmounts()), recipe.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount))
            },
            singleOutput<ItemStackToChemicalRecipe<*, *>> { recipe ->
                build(recipe.id, listOf(recipe.input.toAlternativeMatterAmounts()), recipe.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount))
            },
            singleOutput<ChemicalToChemicalRecipe<*, *, *>> { recipe ->
                build(recipe.id, listOf(recipe.input.toAlternativeMatterAmounts()), recipe.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount))
            },
            singleOutput<ChemicalChemicalToChemicalRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.leftInput.toAlternativeMatterAmounts(),
                        recipe.rightInput.toAlternativeMatterAmounts(),
                    ),
                    recipe.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<ItemStackChemicalToItemStackRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.itemInput.toAlternativeMatterAmounts(),
                        recipe.chemicalInput.toAlternativeMatterAmounts(recipe.chemicalAmountScale()),
                    ),
                    recipe.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount),
                )
            },
            singleOutput<FluidChemicalToChemicalRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.fluidInput.toAlternativeMatterAmounts(),
                        recipe.chemicalInput.toAlternativeMatterAmounts(),
                    ),
                    recipe.outputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<ChemicalCrystallizerRecipe> { recipe ->
                build(recipe.id, listOf(recipe.input.toAlternativeMatterAmounts()), recipe.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount))
            },
            singleOutput<ChemicalDissolutionRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.itemInput.toAlternativeMatterAmounts(),
                        recipe.gasInput.toAlternativeMatterAmounts(),
                    ),
                    recipe.outputDefinition.singleOrNull()?.chemicalStack?.let(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<CombinerRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.mainInput.toAlternativeMatterAmounts(),
                        recipe.extraInput.toAlternativeMatterAmounts(),
                    ),
                    recipe.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::itemAmount),
                )
            },
            singleOutput<FluidToFluidRecipe> { recipe ->
                build(recipe.id, listOf(recipe.input.toAlternativeMatterAmounts()), recipe.outputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::fluidAmount))
            },
            object : RecipeConversionMapper<Recipe<*>> {
                override fun supports(recipe: Any): Boolean = recipe is RotaryRecipe

                override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
                    recipe as RotaryRecipe
                    // Rotary needs both directions available, but treating them as unrelated edges
                    // lets the iterative solver walk back through the same physical conversion pair.
                    // We give both directions the same loopGuardKey so one derivation chain can only
                    // cross that reversible boundary once.
                    val loopGuardKey = recipe.id.withSuffix("rotary_loop_guard").toLite()
                    if (recipe.hasFluidToGas()) {
                        val output = recipe.gasOutputDefinition.singleMatterAmountOrNull(MekanismNodeResolver::chemicalAmount)
                        if (output != null) {
                            ForgeRecipeConversionSupport.addConversionsForAlternatives(
                                id = recipe.id.withSuffix("fluid_to_gas"),
                                consumeAlternatives = listOf(recipe.fluidInput.toAlternativeMatterAmounts()),
                                produces = output,
                                loopGuardKey = loopGuardKey,
                                collector = collector,
                            )
                        }
                    }
                    if (recipe.hasGasToFluid()) {
                        val output = recipe.fluidOutputDefinition.singleMatterAmountOrNull(BuiltinNodeResolver::fluidAmount)
                        if (output != null) {
                            ForgeRecipeConversionSupport.addConversionsForAlternatives(
                                id = recipe.id.withSuffix("gas_to_fluid"),
                                consumeAlternatives = listOf(recipe.gasInput.toAlternativeMatterAmounts()),
                                produces = output,
                                loopGuardKey = loopGuardKey,
                                collector = collector,
                            )
                        }
                    }
                }
            },
            object : RecipeConversionMapper<Recipe<*>> {
                override fun supports(recipe: Any): Boolean = recipe is ElectrolysisRecipe

                override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
                    recipe as ElectrolysisRecipe
                    val output = recipe.outputDefinition.singleOrNull() ?: return
                    val consumeAlternatives = listOf(recipe.input.toAlternativeMatterAmounts())
                    val left = MekanismNodeResolver.chemicalAmount(output.left)
                    val right = MekanismNodeResolver.chemicalAmount(output.right)
                    if (left != null) {
                        ForgeRecipeConversionSupport.addConversionsForAlternatives(
                            id = recipe.id.withSuffix("left"),
                            consumeAlternatives = consumeAlternatives,
                            produces = left,
                            creditsOf = { listOfNotNull(right) },
                            collector = collector,
                        )
                    }
                    if (right != null) {
                        ForgeRecipeConversionSupport.addConversionsForAlternatives(
                            id = recipe.id.withSuffix("right"),
                            consumeAlternatives = consumeAlternatives,
                            produces = right,
                            creditsOf = { listOfNotNull(left) },
                            collector = collector,
                        )
                    }
                }
            },
            object : RecipeConversionMapper<Recipe<*>> {
                override fun supports(recipe: Any): Boolean = recipe is PressurizedReactionRecipe

                override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
                    recipe as PressurizedReactionRecipe
                    val output = recipe.outputDefinition.singleOrNull() ?: return
                    val consumeAlternatives =
                        listOf(
                            recipe.inputSolid.toAlternativeMatterAmounts(),
                            recipe.inputFluid.toAlternativeMatterAmounts(),
                            recipe.inputGas.toAlternativeMatterAmounts(),
                        )
                    val item = BuiltinNodeResolver.itemAmount(output.item)
                    val gas = MekanismNodeResolver.chemicalAmount(output.gas)
                    if (item != null) {
                        ForgeRecipeConversionSupport.addConversionsForAlternatives(
                            id = recipe.id.withSuffix("item"),
                            consumeAlternatives = consumeAlternatives,
                            produces = item,
                            creditsOf = { listOfNotNull(gas) },
                            collector = collector,
                        )
                    }
                    if (gas != null) {
                        ForgeRecipeConversionSupport.addConversionsForAlternatives(
                            id = recipe.id.withSuffix("gas"),
                            consumeAlternatives = consumeAlternatives,
                            produces = gas,
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
        crossinline extractor: (R) -> ConversionInputs?,
    ): RecipeConversionMapper<Recipe<*>> =
        object : RecipeConversionMapper<Recipe<*>> {
            override fun supports(recipe: Any): Boolean = recipe is R

            override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
                val inputs = extractor(recipe as R) ?: return
                ForgeRecipeConversionSupport.addConversionsForAlternatives(
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
    private fun ChemicalStackIngredient<*, *>.toAlternativeMatterAmounts(scale: Long = 1L): List<MatterAmount> =
        ingredientToAlternativeMatterAmounts(
            ingredient = this as InputIngredient<ChemicalStack<*>>,
            nodeOf = MekanismNodeResolver::chemicalNode,
            scale = scale,
        )

    private fun <T> ingredientToAlternativeMatterAmounts(
        ingredient: InputIngredient<T>,
        nodeOf: (T) -> com.p_nsk.replicated_integration.api.MatterNodeKey?,
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

    private fun ItemStackChemicalToItemStackRecipe<*, *, *>.chemicalAmountScale(): Long =
        if (this is ItemStackGasToItemStackRecipe && ForgeRegistries.RECIPE_TYPES.getKey(type) == COMPRESSING_RECIPE_TYPE_ID) {
            COMPRESSING_GAS_SCALE
        } else {
            1L
        }

    private fun <T> List<T>.singleMatterAmountOrNull(mapper: (T) -> MatterAmount?): MatterAmount? =
        singleOrNull()?.let(mapper)

    private fun ResourceLocation.withSuffix(suffix: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, "$path/$suffix")

    private fun ResourceLocation.toLite() =
        ForgeRecipeConversionSupport.run { toLite() }

    private data class ConversionInputs(
        val id: ResourceLocation,
        val consumeAlternatives: List<List<MatterAmount>>,
        val produces: MatterAmount,
        val creditsOf: (List<MatterAmount>) -> List<MatterAmount> = { emptyList() },
    )
}
