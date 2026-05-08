package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.graph.IConversionSink
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
    private val ingredientAlternativesCache = linkedMapOf<String, List<NodeAmount>>()

    fun addConversion(
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

    fun addConversionsForAlternatives(
        id: ResourceLocation,
        consumeAlternatives: List<List<NodeAmount>>,
        produces: NodeAmount,
        creditsOf: (List<NodeAmount>) -> List<NodeAmount> = { emptyList() },
        loopGuardKey: LiteResourceLocation? = null,
        collector: IConversionSink,
    ): Boolean {
        val normalized = consumeAlternatives
            .map(::normalizeAlternatives)
            .filter { it.isNotEmpty() }
        if (normalized.size != consumeAlternatives.size) {
            return false
        }

        val collapsed = collapseEquivalentAlternatives(normalized)

        var combinationCount = 1L
        for ((alternatives, _) in collapsed) {
            combinationCount *= alternatives.size.toLong()
            if (combinationCount > MAX_CONVERSIONS_PER_RECIPE) {
                return false
            }
        }

        if (collapsed.size == 1 && collapsed.single().first.size == 1) {
            val (alternatives, multiplier) = collapsed.single()
            val only = alternatives.single()
            val consumes = listOf(NodeAmount(only.node, only.amount * multiplier))
            addConversion(id, consumes, produces, creditsOf(consumes), loopGuardKey, collector)
            return true
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
                addConversion(conversionId, consumes, produces, creditsOf(consumes), loopGuardKey, collector)
                index++
                return
            }

            val (alternatives, multiplier) = collapsed[depth]
            for (alternative in alternatives) {
                selected += NodeAmount(alternative.node, alternative.amount * multiplier)
                visit(depth + 1, selected)
                selected.removeAt(selected.lastIndex)
            }
        }

        visit(0, mutableListOf())
        return true
    }

    fun ingredientToAlternativeMatterAmounts(
        ingredient: Ingredient,
        nodeOf: (ItemStack) -> NodeKey?,
    ): List<NodeAmount> {
        val key =
            ingredient.items
                .joinToString("|") { stack ->
                    "${BuiltInRegistries.ITEM.getKey(stack.item)}#${stack.count.coerceAtLeast(1)}"
                }
        return ingredientAlternativesCache.getOrPut(key) {
            ingredient.items
                .mapNotNull { stack ->
                    val node = nodeOf(stack) ?: return@mapNotNull null
                    val amount = stack.count.coerceAtLeast(1)
                    NodeAmount(node, amount.toLong())
                }
                .let(::normalizeAlternatives)
        }
    }

    private fun normalizeAlternatives(alternatives: List<NodeAmount>): List<NodeAmount> =
        alternatives
            .groupBy { it.node }
            .map { (node, amounts) ->
                NodeAmount(node, amounts.minOf { it.amount })
            }
            .sortedBy { it.node }

    private fun collapseEquivalentAlternatives(
        alternatives: List<List<NodeAmount>>,
    ): List<Pair<List<NodeAmount>, Long>> =
        alternatives
            .groupingBy(::alternativesSignature)
            .eachCount()
            .map { (signature, count) ->
                val representative =
                    alternatives.first { candidate ->
                        alternativesSignature(candidate) == signature
                    }
                representative to count.toLong()
            }
            .sortedBy { (candidate, _) ->
                candidate.joinToString("|") { "${it.node}:${it.amount}" }
            }

    private fun alternativesSignature(alternatives: List<NodeAmount>): String =
        alternatives.joinToString("|") { "${it.node}:${it.amount}" }

    fun ResourceLocation.toLite(): LiteResourceLocation =
        LiteResourceLocation.of(namespace, path)
}
