package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.InputNodes
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.model.NodeAmount
import com.p_nsk.replicated_integration.api.model.MatterConversion
import com.p_nsk.replicated_integration.api.node.NodeKey
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient

object NeoRecipeConversionSupport {
    const val MAX_CONVERSIONS_PER_RECIPE = 256L
    private val ingredientInputNodesCache = linkedMapOf<String, InputNodes>()

    private fun addConversionInternal(
        id: ResourceLocation,
        consumes: List<NodeAmount>,
        produces: NodeAmount,
        credits: List<NodeAmount> = emptyList(),
        loopGuardKey: LiteResourceLocation? = null,
        collector: IConversionSink,
    ) {
        if (consumes.isEmpty()) {
            return
        }
        collector.add(
            MatterConversion(
                id = id.toLite(),
                consumes = consumes,
                produces = produces,
                credits = credits,
                loopGuardKey = loopGuardKey,
            )
        )
    }

    fun addConversion(
        id: ResourceLocation,
        consumeInputNodes: List<InputNodes>,
        produces: NodeAmount,
        creditsOf: (List<NodeAmount>) -> List<NodeAmount> = { emptyList() },
        loopGuardKey: LiteResourceLocation? = null,
        collector: IConversionSink,
    ): Boolean {
        if (consumeInputNodes.any { it.isEmpty() }) {
            return false
        }

        val collapsed = collapseEquivalentInputs(consumeInputNodes)

        var combinationCount = 1L
        for ((inputNode, _) in collapsed) {
            combinationCount *= inputNode.size.toLong()
            if (combinationCount > MAX_CONVERSIONS_PER_RECIPE) {
                return false
            }
        }

        if (collapsed.size == 1) {
            val (inputNode, multiplier) = collapsed.single()
            if (inputNode.size == 1) {
                val only = inputNode.single()
                val consumes = listOf(NodeAmount(only.node, only.amount * multiplier))
                addConversionInternal(id, consumes, produces, creditsOf(consumes), loopGuardKey, collector)
                return true
            }
        }

        val suffixWidth = combinationCount.toString().length
        var index = 0

        fun visit(depth: Int, selected: MutableList<NodeAmount>) {
            if (depth == collapsed.size) {
                val conversionId =
                    if (index == 0) {
                        id
                    } else {
                        ResourceLocation.fromNamespaceAndPath(
                            id.namespace,
                            "${id.path}/alt_${index.toString().padStart(suffixWidth, '0')}",
                        )
                    }
                val consumes = selected.toList()
                addConversionInternal(conversionId, consumes, produces, creditsOf(consumes), loopGuardKey, collector)
                index++
                return
            }

            val (inputNode, multiplier) = collapsed[depth]
            for (choice in inputNode) {
                selected += NodeAmount(choice.node, choice.amount * multiplier)
                visit(depth + 1, selected)
                selected.removeAt(selected.lastIndex)
            }
        }

        visit(0, mutableListOf())
        return true
    }

    fun ingredientToInputNode(
        ingredient: Ingredient,
        nodeOf: (ItemStack) -> NodeKey?,
    ): InputNodes {
        val key =
            ingredient.items
                .joinToString("|") { stack ->
                    "${BuiltInRegistries.ITEM.getKey(stack.item)}#${stack.count.coerceAtLeast(1)}"
                }
        return ingredientInputNodesCache.getOrPut(key) {
            ingredient.items
                .mapNotNull { stack ->
                    val node = nodeOf(stack) ?: return@mapNotNull null
                    val amount = stack.count.coerceAtLeast(1)
                    NodeAmount(node, amount.toLong())
                }
                .let(::InputNodes)
        }
    }

    private fun collapseEquivalentInputs(
        inputs: List<InputNodes>,
    ): List<Pair<InputNodes, Long>> =
        inputs
            .groupingBy { it }
            .eachCount()
            .map { (input, count) -> input to count.toLong() }
            .sortedBy { (candidate, _) -> candidate.signature }

    fun invalidateCache() {
        ingredientInputNodesCache.clear()
    }

    fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
