package com.p_nsk.replicated_integration.core

import com.p_nsk.replicated_integration.api.addon.ReplicationAddon
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterNodeDef
import com.p_nsk.replicated_integration.api.node.MatterNodeRegistry

typealias ForgeMatterNodeRegistry = MatterNodeRegistry<ForgeMatterCommand>

fun ForgeMatterNodeRegistry.node(
    id: LiteResourceLocation,
    displayName: String,
    formatter: (LiteResourceLocation) -> String = LiteResourceLocation::toString,
    block: ForgeMatterNodeBuilder.() -> Unit = {},
) {
    val builder = ForgeMatterNodeBuilder(id)
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
typealias ForgeReplicationAddon =
        ReplicationAddon<ForgeReplicationAddonContext, ForgeMatterCommand>
