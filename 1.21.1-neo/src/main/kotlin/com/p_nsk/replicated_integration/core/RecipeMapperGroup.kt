package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.api.model.NodeAmount
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder

abstract class RecipeMapperGroup {
    fun build(
        id: ResourceLocation,
        consumeInputNodes: List<InputNodes>,
        produces: NodeAmount?,
    ): ConversionInputs? = produces?.let { ConversionInputs(id, consumeInputNodes, it) }

    inline fun <reified R : Recipe<*>> singleOutput(
        crossinline extractor: (RecipeHolder<R>) -> ConversionInputs?,
    ): RecipeConversionMapper<RecipeHolder<*>> =
        object : RecipeConversionMapper<RecipeHolder<*>> {
            override fun supports(recipe: Any): Boolean = recipe is RecipeHolder<*> && recipe.value is R

            @Suppress("UNCHECKED_CAST")
            override fun collect(recipe: RecipeHolder<*>, collector: IConversionSink) {
                val inputs = extractor(recipe as RecipeHolder<R>) ?: return
                NeoRecipeConversionSupport.addConversion(
                    id = inputs.id,
                    consumeInputNodes = inputs.consumeInputNodes,
                    produces = inputs.produces,
                    creditsOf = inputs.creditsOf,
                    collector = collector,
                )
            }
        }

    data class ConversionInputs(
        val id: ResourceLocation,
        val consumeInputNodes: List<InputNodes>,
        val produces: NodeAmount,
        val creditsOf: (List<NodeAmount>) -> List<NodeAmount> = { emptyList() },
    )
}

fun Ingredient.toInputNode(): InputNodes =
    NeoRecipeConversionSupport.ingredientToInputNode(this, BuiltinNodeResolver::itemNode)
