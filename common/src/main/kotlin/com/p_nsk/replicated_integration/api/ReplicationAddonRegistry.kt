package com.p_nsk.replicated_integration.api

class ReplicationAddonRegistry<C>(
    addons: Iterable<ReplicationAddon<C>>,
    private val nodeTypeRegistry: MatterNodeTypeRegistry = MatterNodeTypeRegistry.withDefaults(),
) {
    private val addons = addons.toList()

    init {
        this.addons.forEach { addon ->
            addon.registerNodeTypes(nodeTypeRegistry)
        }
    }

    fun active(environment: ReplicationAddonEnvironment): List<ReplicationAddon<C>> =
        addons.filter { it.isEnabled(environment) }

    fun nodeTypes(): MatterNodeTypeRegistry = nodeTypeRegistry
}
