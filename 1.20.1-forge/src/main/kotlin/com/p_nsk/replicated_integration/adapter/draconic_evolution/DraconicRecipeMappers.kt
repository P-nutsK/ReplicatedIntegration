package com.p_nsk.replicated_integration.adapter.draconic_evolution

import com.brandon3055.draconicevolution.api.crafting.FusionRecipe
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.core.RecipeMapperGroup
import com.p_nsk.replicated_integration.core.toInputNode
import net.minecraft.core.RegistryAccess
import net.minecraft.world.item.crafting.Recipe

object DraconicRecipeMappers : RecipeMapperGroup() {
    val all: List<RecipeConversionMapper<Recipe<*>>> = listOf(
        singleOutput<FusionRecipe> { recipe ->
            val consumes = buildList {
                recipe.fusionIngredients().filter { it.consume() }.mapTo(this) { it.get().toInputNode() }

                recipe.catalyst.toInputNode().let(::add)
            }

            build(
                recipe.id,
                consumes,
                BuiltinNodeResolver.itemAmount(recipe.getResultItem(RegistryAccess.EMPTY)),
            )
        },

        )


}
