package com.p_nsk.replicated_integration.adapter.ae2

import appeng.recipes.handlers.ChargerRecipe
import appeng.recipes.handlers.InscriberProcessType
import appeng.recipes.handlers.InscriberRecipe
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.core.RecipeMapperGroup
import com.p_nsk.replicated_integration.core.toInputNode
import net.minecraft.world.item.crafting.RecipeHolder

object Ae2RecipeMappers : RecipeMapperGroup() {
    val all: List<RecipeConversionMapper<RecipeHolder<*>>> = listOf(
        singleOutput<InscriberRecipe> { holder ->
            val recipe = holder.value
            val consumes = when (recipe.processType) {
                InscriberProcessType.PRESS -> listOfNotNull(
                    recipe.topOptional?.toInputNode(),
                    recipe.middleInput.toInputNode(),
                    recipe.bottomOptional?.toInputNode(),
                )

                InscriberProcessType.INSCRIBE -> listOf(recipe.middleInput.toInputNode())
            }

            build(
                holder.id,
                consumes,
                BuiltinNodeResolver.itemAmount(recipe.resultItem),
            )
        },
        singleOutput<ChargerRecipe> { holder ->
            val recipe = holder.value
            build(
                holder.id,
                listOf(recipe.ingredient.toInputNode()),
                BuiltinNodeResolver.itemAmount(recipe.resultItem),
            )
        },
    )
}
