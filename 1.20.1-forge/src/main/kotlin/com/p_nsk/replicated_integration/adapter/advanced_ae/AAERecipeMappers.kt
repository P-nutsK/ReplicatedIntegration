package com.p_nsk.replicated_integration.adapter.advanced_ae

import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.core.ForgeRecipeConversionSupport
import com.p_nsk.replicated_integration.core.RecipeMapperGroup
import net.minecraft.world.item.crafting.Recipe
import net.minecraftforge.fluids.FluidStack
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe
import net.pedroksl.ae2addonlib.recipes.IngredientStack

object AAERecipeMappers : RecipeMapperGroup() {
    val all: List<RecipeConversionMapper<Recipe<*>>> = listOf(
        object : RecipeConversionMapper<Recipe<*>> {
            override fun supports(recipe: Any): Boolean = recipe is ReactionChamberRecipe

            override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
                recipe as ReactionChamberRecipe
                val items = recipe.inputs.map { it.toInputNode() }
                val consumes: List<InputNodes> = buildList {
                    addAll(items)
                    recipe.fluid?.toInputNode()?.let(::add)
                }
                val produces =
                    BuiltinNodeResolver.itemAmount(recipe.resultItem)
                        ?: BuiltinNodeResolver.fluidAmount(recipe.resultFluid)
                        ?: run {
                            Constants.LOGGER.warn("Recipe {} has no valid output; skipping", recipe.id)
                            return
                        }
                ForgeRecipeConversionSupport.addConversion(
                    id = recipe.id,
                    consumeInputNodes = consumes,
                    produces = produces,
                    collector = collector,
                )
            }
        },
    )

    private fun IngredientStack.Item.toInputNode(): InputNodes = ForgeRecipeConversionSupport.ingredientToInputNode(
        this.ingredient, BuiltinNodeResolver::itemNode, this.amount.toLong()
    )

    private fun IngredientStack.Fluid.toInputNode(): InputNodes? {
        val nodeAmount = BuiltinNodeResolver.fluidAmount(FluidStack(this.ingredient, this.amount))
        return nodeAmount?.let { InputNodes(listOf(it)) }
    }


}

