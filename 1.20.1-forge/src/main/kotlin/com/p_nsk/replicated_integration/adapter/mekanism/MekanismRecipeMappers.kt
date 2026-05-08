package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.core.ForgeRecipeConversionSupport
import com.p_nsk.replicated_integration.core.RecipeMapperGroup
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

object MekanismRecipeMappers: RecipeMapperGroup() {
    private const val COMPRESSING_GAS_SCALE = 200L
    private val COMPRESSING_RECIPE_TYPE_ID = ResourceLocation.fromNamespaceAndPath("mekanism", "compressing")

    val all: List<RecipeConversionMapper<Recipe<*>>> =
        listOf(
            singleOutput<ItemStackToItemStackRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(recipe.input.toInputNode()),
                    recipe.outputDefinition.singleNodeAmountOrNull(BuiltinNodeResolver::itemAmount)
                )
            },
            singleOutput<ItemStackToChemicalRecipe<*, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(recipe.input.toInputNode()),
                    recipe.outputDefinition.singleNodeAmountOrNull(MekanismNodeResolver::chemicalAmount)
                )
            },
            singleOutput<ChemicalToChemicalRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(recipe.input.toInputNode()),
                    recipe.outputDefinition.singleNodeAmountOrNull(MekanismNodeResolver::chemicalAmount)
                )
            },
            singleOutput<ChemicalChemicalToChemicalRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.leftInput.toInputNode(),
                        recipe.rightInput.toInputNode(),
                    ),
                    recipe.outputDefinition.singleNodeAmountOrNull(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<ItemStackChemicalToItemStackRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.itemInput.toInputNode(),
                        recipe.chemicalInput.toInputNode(recipe.chemicalAmountScale()),
                    ),
                    recipe.outputDefinition.singleNodeAmountOrNull(BuiltinNodeResolver::itemAmount),
                )
            },
            singleOutput<FluidChemicalToChemicalRecipe<*, *, *>> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.fluidInput.toInputNode(),
                        recipe.chemicalInput.toInputNode(),
                    ),
                    recipe.outputDefinition.singleNodeAmountOrNull(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<ChemicalCrystallizerRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(recipe.input.toInputNode()),
                    recipe.outputDefinition.singleNodeAmountOrNull(BuiltinNodeResolver::itemAmount)
                )
            },
            singleOutput<ChemicalDissolutionRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.itemInput.toInputNode(),
                        recipe.gasInput.toInputNode(),
                    ),
                    recipe.outputDefinition.singleOrNull()?.chemicalStack?.let(MekanismNodeResolver::chemicalAmount),
                )
            },
            singleOutput<CombinerRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(
                        recipe.mainInput.toInputNode(),
                        recipe.extraInput.toInputNode(),
                    ),
                    recipe.outputDefinition.singleNodeAmountOrNull(BuiltinNodeResolver::itemAmount),
                )
            },
            singleOutput<FluidToFluidRecipe> { recipe ->
                build(
                    recipe.id,
                    listOf(recipe.input.toInputNode()),
                    recipe.outputDefinition.singleNodeAmountOrNull(BuiltinNodeResolver::fluidAmount)
                )
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
                        val output =
                            recipe.gasOutputDefinition.singleNodeAmountOrNull(MekanismNodeResolver::chemicalAmount)
                        if (output != null) {
                            ForgeRecipeConversionSupport.addConversion(
                                id = recipe.id.withSuffix("fluid_to_gas"),
                                consumeInputNodes = listOf(recipe.fluidInput.toInputNode()),
                                produces = output,
                                loopGuardKey = loopGuardKey,
                                collector = collector,
                            )
                        }
                    }
                    if (recipe.hasGasToFluid()) {
                        val output =
                            recipe.fluidOutputDefinition.singleNodeAmountOrNull(BuiltinNodeResolver::fluidAmount)
                        if (output != null) {
                            ForgeRecipeConversionSupport.addConversion(
                                id = recipe.id.withSuffix("gas_to_fluid"),
                                consumeInputNodes = listOf(recipe.gasInput.toInputNode()),
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
                    val consumeAlternatives = listOf(recipe.input.toInputNode())
                    val left = MekanismNodeResolver.chemicalAmount(output.left)
                    val right = MekanismNodeResolver.chemicalAmount(output.right)
                    if (left != null) {
                        ForgeRecipeConversionSupport.addConversion(
                            id = recipe.id.withSuffix("left"),
                            consumeInputNodes = consumeAlternatives,
                            produces = left,
                            creditsOf = { listOfNotNull(right) },
                            collector = collector,
                        )
                    }
                    if (right != null) {
                        ForgeRecipeConversionSupport.addConversion(
                            id = recipe.id.withSuffix("right"),
                            consumeInputNodes = consumeAlternatives,
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
                    val consumes =
                        listOf(
                            recipe.inputSolid.toInputNode(),
                            recipe.inputFluid.toInputNode(),
                            recipe.inputGas.toInputNode(),
                        )
                    val item = BuiltinNodeResolver.itemAmount(output.item)
                    val gas = MekanismNodeResolver.chemicalAmount(output.gas)
                    if (item != null) {
                        ForgeRecipeConversionSupport.addConversion(
                            id = recipe.id.withSuffix("item"),
                            consumeInputNodes = consumes,
                            produces = item,
                            creditsOf = { listOfNotNull(gas) },
                            collector = collector,
                        )
                    }
                    if (gas != null) {
                        ForgeRecipeConversionSupport.addConversion(
                            id = recipe.id.withSuffix("gas"),
                            consumeInputNodes = consumes,
                            produces = gas,
                            creditsOf = { listOfNotNull(item) },
                            collector = collector,
                        )
                    }
                }
            },
        )

    private fun ItemStackIngredient.toInputNode(): InputNodes =
        ingredientToInputNode(
            ingredient = this as InputIngredient<ItemStack>,
            nodeOf = BuiltinNodeResolver::itemNode,
        )

    private fun FluidStackIngredient.toInputNode(): InputNodes =
        ingredientToInputNode(
            ingredient = this as InputIngredient<FluidStack>,
            nodeOf = BuiltinNodeResolver::fluidNode,
        )

    @Suppress("UNCHECKED_CAST")
    private fun ChemicalStackIngredient<*, *>.toInputNode(scale: Long = 1L): InputNodes =
        ingredientToInputNode(
            ingredient = this as InputIngredient<ChemicalStack<*>>,
            nodeOf = MekanismNodeResolver::chemicalNode,
            scale = scale,
        )

    private fun <T> ingredientToInputNode(
        ingredient: InputIngredient<T>,
        nodeOf: (T) -> NodeKey?,
        scale: Long = 1L,
    ): InputNodes =
        ingredient.representations.mapNotNull { representation ->
            val node = nodeOf(representation) ?: return@mapNotNull null
            val needed = ingredient.getNeededAmount(representation) * scale
            if (needed <= 0) {
                null
            } else {
                NodeAmount(node, needed)
            }
        }.let(::InputNodes)
    // mekの変な仕様である圧縮機のガスの要求量は200倍であるというやつに対応する
    private fun ItemStackChemicalToItemStackRecipe<*, *, *>.chemicalAmountScale(): Long =
        if (this is ItemStackGasToItemStackRecipe && ForgeRegistries.RECIPE_TYPES.getKey(type) == COMPRESSING_RECIPE_TYPE_ID) {
            COMPRESSING_GAS_SCALE
        } else {
            1L
        }

    private fun <T> List<T>.singleNodeAmountOrNull(mapper: (T) -> NodeAmount?): NodeAmount? =
        singleOrNull()?.let(mapper)

    private fun ResourceLocation.withSuffix(suffix: String): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(namespace, "$path/$suffix")

    private fun ResourceLocation.toLite() =
        ForgeRecipeConversionSupport.run { toLite() }

}
