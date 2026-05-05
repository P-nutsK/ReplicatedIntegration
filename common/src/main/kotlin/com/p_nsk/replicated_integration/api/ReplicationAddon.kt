package com.p_nsk.replicated_integration.api

interface ReplicationAddonEnvironment {
    fun isModLoaded(modId: String): Boolean
}

interface ReplicationAddon<C> {
    val id: String

    fun isEnabled(environment: ReplicationAddonEnvironment): Boolean = true

    fun registerNodeTypes(registry: MatterNodeTypeRegistry) = Unit

    fun collectDefaults(context: C, defaults: MutableMatterDefaults) = Unit

    fun collectConversions(context: C, collector: IConversionSink) = Unit
}
