package com.p_nsk.replicated_integration.api.model

/**
 * Recipeそのものを錬成用にしたみたいな感じ
 * 必ずシングルアウトプットになる。
 * 複数アウトプットの場合はcreditで他の副産物の分のマターを差し引きして複数作る。
 */
@JvmRecord
data class MatterConversion(
    // our Recipe Id
    val id: LiteResourceLocation,
    //
    val consumes: List<NodeAmount>,
    val produces: NodeAmount,
    // 副産物の分の差し引き
    val credits: List<NodeAmount> = emptyList(),
    // This is not general recipe metadata; it exists specifically so the solver can reject
    // re-entering the same reversible conversion pair within a single derivation chain.
    val loopGuardKey: LiteResourceLocation? = null,
) {
    init {
        require(consumes.isNotEmpty()) { "consumes must not be empty" }
    }
}
