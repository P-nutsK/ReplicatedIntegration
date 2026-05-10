package com.p_nsk.replicated_integration.adapter.draconic_evolution

import com.brandon3055.draconicevolution.api.crafting.FusionRecipe
import com.p_nsk.replicated_integration.adapter.vanilla.BuiltinNodeResolver
import com.p_nsk.replicated_integration.api.graph.RecipeConversionMapper
import com.p_nsk.replicated_integration.core.RecipeMapperGroup
import com.p_nsk.replicated_integration.core.toInputNode
import net.minecraft.core.RegistryAccess
import net.minecraft.world.item.crafting.RecipeHolder

object DraconicRecipeMappers : RecipeMapperGroup() {
    val all: List<RecipeConversionMapper<RecipeHolder<*>>> = listOf(
        singleOutput<FusionRecipe> { holder ->
            val recipe = holder.value
            val consumes = buildList {
                recipe.fusionIngredients().filter { it.consume() }.mapTo(this) { it.get().toInputNode() }
                add(recipe.catalyst.toInputNode())
            }

            build(
                holder.id,
                consumes,
                BuiltinNodeResolver.itemAmount(recipe.getResultItem(RegistryAccess.EMPTY)),
            )
        },
    )
}
