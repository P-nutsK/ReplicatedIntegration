package com.p_nsk.replicated_integration.core


import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.api.model.NodeAmount
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe

abstract class RecipeMapperGroup {

    fun build(
        id: ResourceLocation,
        consumeAlternatives: List<InputNodes>,
        produces: NodeAmount?,
    ): ConversionInputs? = produces?.let { ConversionInputs(id, consumeAlternatives, it) }

    inline fun <reified R : Recipe<*>> singleOutput(
        crossinline extractor: (R) -> ConversionInputs?,
    ): RecipeConversionMapper<Recipe<*>> = object : RecipeConversionMapper<Recipe<*>> {
        override fun supports(recipe: Any): Boolean = recipe is R

        override fun collect(recipe: Recipe<*>, collector: IConversionSink) {
            val inputs = extractor(recipe as R) ?: return
            ForgeRecipeConversionSupport.addConversion(
                id = inputs.id,
                consumeInputNodes = inputs.consumeAlternatives,
                produces = inputs.produces,
                creditsOf = inputs.creditsOf,
                collector = collector,
            )
        }
    }

    data class ConversionInputs(
        val id: ResourceLocation,
        val consumeAlternatives: List<InputNodes>,
        val produces: NodeAmount,
        val creditsOf: (List<NodeAmount>) -> List<NodeAmount> = { emptyList() },
    )
}

fun ResourceLocation.toLite() = ForgeRecipeConversionSupport.run { toLite() }
fun Ingredient.toInputNode(): InputNodes =
    ForgeRecipeConversionSupport.ingredientToInputNode(this, BuiltinNodeResolver::itemNode)
