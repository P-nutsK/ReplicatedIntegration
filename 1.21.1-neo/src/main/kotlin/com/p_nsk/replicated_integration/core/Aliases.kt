package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.addon.ReplicationAddon
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterNodeDef
import com.p_nsk.replicated_integration.api.node.MatterNodeRegistry

typealias NeoMatterNodeRegistry = MatterNodeRegistry<NeoMatterCommand>

fun NeoMatterNodeRegistry.node(
    id: LiteResourceLocation,
    displayName: String,
    formatter: (LiteResourceLocation) -> String = LiteResourceLocation::toString,
    block: NeoMatterNodeBuilder.() -> Unit = {},
) {
    val builder = NeoMatterNodeBuilder(id)
    builder.block()

    register(
        MatterNodeDef(
            id = id,
            displayName = displayName,
            formatter = formatter,
            commands = builder.buildCommands(),
        )
    )
}

@OptIn(ReplicationAddonLoadSafetyContract::class)
typealias NeoReplicationAddon =
    ReplicationAddon<NeoReplicationAddonContext, NeoMatterCommand>
