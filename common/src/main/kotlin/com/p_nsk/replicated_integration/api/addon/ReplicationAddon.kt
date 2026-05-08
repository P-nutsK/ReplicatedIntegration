package com.p_nsk.replicated_integration.api.addon

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.node.MatterNodeTypeRegistry
import com.p_nsk.replicated_integration.api.node.MutableMatterDefaults
import com.p_nsk.replicated_integration.api.selector.MutableMatterSelectors

interface ReplicationAddonEnvironment {
    fun isModLoaded(modId: String): Boolean
}
@ReplicationAddonLoadSafetyContract
interface ReplicationAddon<C> {
    val id: String

    fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    fun registerNodeTypes(registry: MatterNodeTypeRegistry) = Unit

    fun collectDefaults(context: C, defaults: MutableMatterDefaults) = Unit

    fun collectSelectors(context: C, selectors: MutableMatterSelectors) = Unit

    fun collectConversions(context: C, collector: IConversionSink) = Unit
}
