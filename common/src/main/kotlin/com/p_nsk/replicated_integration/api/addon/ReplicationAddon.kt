package com.p_nsk.replicated_integration.api.addon

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.node.MatterCommandDef
import com.p_nsk.replicated_integration.api.node.MatterNodeRegistry
import com.p_nsk.replicated_integration.api.node.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.selector.MutableMatterSelectors

interface ReplicationAddonEnvironment {
    fun isModLoaded(modId: String): Boolean
}
@ReplicationAddonLoadSafetyContract
interface ReplicationAddon<C, N : MatterCommandDef> {
    val id: String

    fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    fun registerMatterNodes(registry: MatterNodeRegistry<N>) = Unit

    fun collectDefaults(context: C, defaults: MutableMatterDefaults) = Unit

    fun collectSelectors(context: C, selectors: MutableMatterSelectors) = Unit

    fun collectConversions(context: C, collector: IConversionSink) = Unit
}
