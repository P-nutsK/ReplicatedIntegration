package com.p_nsk.replicated_integration.debug

import com.p_nsk.replicated_integration.api.ConversionGraph
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.MatterNodeKey

object MatterNodeDebugCache {
    @Volatile
    private var snapshot: Snapshot = Snapshot.EMPTY

    fun publish(
        explicitValues: Map<MatterNodeKey, ExplicitMatterValue>,
        graph: ConversionGraph,
        solved: Map<MatterNodeKey, LiteMatterCompound>,
    ) {
        snapshot = Snapshot(explicitValues.toMap(), graph, solved.toMap())
    }

    fun isEmpty(): Boolean = snapshot.solved.isEmpty()

    fun get(node: MatterNodeKey): LiteMatterCompound? = snapshot.solved[node]

    fun explicit(node: MatterNodeKey): ExplicitMatterValue? = snapshot.explicitValues[node]

    fun graph(): ConversionGraph = snapshot.graph

    fun solved(): Map<MatterNodeKey, LiteMatterCompound> = snapshot.solved

    data class Snapshot(
        val explicitValues: Map<MatterNodeKey, ExplicitMatterValue>,
        val graph: ConversionGraph,
        val solved: Map<MatterNodeKey, LiteMatterCompound>,
    ) {
        companion object {
            val EMPTY = Snapshot(emptyMap(), ConversionGraph(emptyList()), emptyMap())
        }
    }
}
