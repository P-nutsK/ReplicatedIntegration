package com.p_nsk.replicated_integration.adapter.advanced_ae

import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.core.NeoRecipeConversionSupport
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.fluids.FluidStack
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipe
import net.pedroksl.ae2addonlib.recipes.IngredientStack

object AAERecipeMappers {
    val all: List<RecipeConversionMapper<RecipeHolder<*>>> = listOf(
        object : RecipeConversionMapper<RecipeHolder<*>> {
            override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is ReactionChamberRecipe

            @Suppress("UNCHECKED_CAST")
            override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                val holder = recipe as RecipeHolder<ReactionChamberRecipe>
                val reaction = holder.value
                val consumes = buildList {
                    addAll(reaction.inputs.map { it.toInputNode() })
                    reaction.fluidInputNode()?.let(::add)
                }
                val produces =
                    BuiltinNodeResolver.itemAmount(reaction.resultItem)
                        ?: BuiltinNodeResolver.fluidAmount(reaction.resultFluid)
                        ?: run {
                            Constants.LOGGER.warn("Recipe {} has no valid output; skipping", holder.id)
                            return
                        }
                NeoRecipeConversionSupport.addConversion(
                    id = holder.id,
                    consumeInputNodes = consumes,
                    produces = produces,
                    collector = collector,
                )
            }
        },
    )

    private fun IngredientStack.Item.toInputNode(): InputNodes =
        NeoRecipeConversionSupport.ingredientToInputNode(
            ingredientStackField("ingredient") as Ingredient,
            BuiltinNodeResolver::itemNode,
        ).map { NodeAmount(it.node, it.amount * (ingredientStackField("amount") as Int).toLong()) }
            .let(::InputNodes)

    private fun ReactionChamberRecipe.fluidInputNode(): InputNodes? {
        val fluidInput = javaClass.getMethod("getFluid").invoke(this) ?: return null
        val fluid = fluidInput.ingredientStackField("ingredient") as? Fluid ?: return null
        val amount = fluidInput.ingredientStackField("amount") as? Int ?: return null
        val nodeAmount = BuiltinNodeResolver.fluidAmount(FluidStack(fluid, amount))
        return nodeAmount?.let { InputNodes(listOf(it)) }
    }

    private fun Any.ingredientStackField(name: String): Any {
        var type: Class<*>? = javaClass
        while (type != null) {
            runCatching { type.getDeclaredField(name) }
                .getOrNull()
                ?.let { field ->
                    field.isAccessible = true
                    return field.get(this)
                }
            type = type.superclass
        }
        error("IngredientStack field '$name' was not found on ${javaClass.name}")
    }
}
