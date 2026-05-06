package com.p_nsk.replicated_integration.debug

import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.ConversionGraph
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.MatterNodeKey
import com.p_nsk.replicated_integration.api.MatterSelectorKey

object MatterNodeDebugCache {
    @Volatile
    private var snapshot: Snapshot = Snapshot.EMPTY

    fun publish(
        selectorValues: Map<MatterSelectorKey, ExplicitMatterValue>,
        explicitValues: Map<MatterNodeKey, ExplicitMatterValue>,
        graph: ConversionGraph,
        solved: Map<MatterNodeKey, LiteMatterCompound>,
    ) {
        snapshot = Snapshot(selectorValues.toMap(), explicitValues.toMap(), graph, solved.toMap())
    }

    fun isEmpty(): Boolean = snapshot.solved.isEmpty()

    fun get(node: MatterNodeKey): LiteMatterCompound? = snapshot.solved[node]

    fun explicit(node: MatterNodeKey): ExplicitMatterValue? = snapshot.explicitValues[node]

    fun selector(selector: MatterSelectorKey): ExplicitMatterValue? = snapshot.selectorValues[selector]

    fun graph(): ConversionGraph = snapshot.graph

    fun solved(): Map<MatterNodeKey, LiteMatterCompound> = snapshot.solved

    data class Snapshot(
        val selectorValues: Map<MatterSelectorKey, ExplicitMatterValue>,
        val explicitValues: Map<MatterNodeKey, ExplicitMatterValue>,
        val graph: ConversionGraph,
        val solved: Map<MatterNodeKey, LiteMatterCompound>,
    ) {
        companion object {
            val EMPTY = Snapshot(emptyMap(), emptyMap(), ConversionGraph(emptyList()), emptyMap())
        }
    }
}
