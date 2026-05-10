package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

data class MatterNodeDef<C : MatterCommandDef>(
    val id: LiteResourceLocation,
    val displayName: String,
    val formatter: (LiteResourceLocation) -> String = LiteResourceLocation::toString,
    val commands: MatterNodeCommands<C> = MatterNodeCommands(),
)

data class MatterNodeCommands<C : MatterCommandDef>(
    val value: C? = null,
    val tag: C? = null,
) {
    val all: List<C>
        get() = listOfNotNull(value, tag)
}

interface MatterCommandDef {
    val literal: String
    val nodeType: LiteResourceLocation
}
