package com.p_nsk.replicated_integration.api.addon

import com.p_nsk.replicated_integration.api.node.MatterCommandDef
import com.p_nsk.replicated_integration.api.node.MatterNodeRegistry

@OptIn(ReplicationAddonLoadSafetyContract::class)
class ReplicationAddonRegistry<C, N : MatterCommandDef>(
    addons: Iterable<ReplicationAddon<C, N>>,
) {
    private val addons = addons.toList()

    fun active(environment: ReplicationAddonEnvironment): List<ReplicationAddon<C, N>> =
        addons.filter { it.isEnabled(environment) }

    fun matterNodes(environment: ReplicationAddonEnvironment): MatterNodeRegistry<N> =
        MatterNodeRegistry<N>().apply {
            active(environment).forEach { addon ->
                addon.registerMatterNodes(this)
            }
        }
}
