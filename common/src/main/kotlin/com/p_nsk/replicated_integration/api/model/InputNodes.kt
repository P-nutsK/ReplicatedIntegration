package com.p_nsk.replicated_integration.api.model

import kotlin.collections.joinToString

/**
 * InputNodes: 多数の候補を持つ入力ノードの集合。例えば、レシピの材料が複数のアイテムで代替可能な場合などに使用される。
 */
class InputNodes(
    choices: List<NodeAmount>,
) : Iterable<NodeAmount> {
    internal val choices = normalize(choices)
    val size: Int
        get() = choices.size

    fun isEmpty(): Boolean = choices.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    // for sort. もっと使うようならクラスをComparableにする
    val signature: String by lazy {
        choices.joinToString("|") { "${it.node}:${it.amount}" }
    }

    override fun equals(other: Any?): Boolean = this === other || other is InputNodes && choices == other.choices

    override fun hashCode(): Int = choices.hashCode()

    override fun toString(): String = "InputMatter($choices)"

    fun single(): NodeAmount {
        return when (size) {
            0 -> throw NoSuchElementException("InputMatter is empty.")
            1 -> choices.single()
            else -> throw IllegalArgumentException("InputMatter has more than one element.")
        }
    }

    override fun iterator(): Iterator<NodeAmount> {
        return choices.iterator()
    }

    companion object {
        private fun normalize(alternatives: List<NodeAmount>): List<NodeAmount> =
            alternatives.filter { it.amount > 0 }.groupBy { it.node }.map { (node, amounts) ->
                NodeAmount(node, amounts.minOf { it.amount })
            }.sortedBy { it.node }
        fun empty(): InputNodes = InputNodes(emptyList())
    }
}
