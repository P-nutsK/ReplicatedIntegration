package com.p_nsk.replicated_integration.debug

import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.graph.ConversionGraph
import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey

object MatterNodeDebugCache {
    @Volatile
    private var snapshot: Snapshot = Snapshot.EMPTY

    fun publish(
        selectorValues: Map<MatterSelectorKey, ExplicitMatterValue>,
        explicitValues: Map<NodeKey, ExplicitMatterValue>,
        graph: ConversionGraph,
        solved: Map<NodeKey, LiteMatterCompound>,
    ) {
        snapshot = Snapshot(selectorValues.toMap(), explicitValues.toMap(), graph, solved.toMap())
    }

    fun isEmpty(): Boolean = snapshot.solved.isEmpty()

    fun get(node: NodeKey): LiteMatterCompound? = snapshot.solved[node]

    fun explicit(node: NodeKey): ExplicitMatterValue? = snapshot.explicitValues[node]

    fun selector(selector: MatterSelectorKey): ExplicitMatterValue? = snapshot.selectorValues[selector]

    fun graph(): ConversionGraph = snapshot.graph

    fun solved(): Map<NodeKey, LiteMatterCompound> = snapshot.solved

    data class Snapshot(
        val selectorValues: Map<MatterSelectorKey, ExplicitMatterValue>,
        val explicitValues: Map<NodeKey, ExplicitMatterValue>,
        val graph: ConversionGraph,
        val solved: Map<NodeKey, LiteMatterCompound>,
    ) {
        companion object {
            val EMPTY = Snapshot(emptyMap(), emptyMap(), ConversionGraph(emptyList()), emptyMap())
        }
    }
}
