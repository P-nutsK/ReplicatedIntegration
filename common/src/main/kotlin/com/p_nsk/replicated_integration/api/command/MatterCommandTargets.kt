package com.p_nsk.replicated_integration.api.command

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterCommandDef
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.selector.MatterSelectorKey

fun MatterCommandDef.nodeKey(id: LiteResourceLocation): NodeKey =
    NodeKey(
        type = nodeType,
        id = id,
    )

fun MatterCommandDef.selectorKey(id: LiteResourceLocation): MatterSelectorKey =
    MatterSelectorKey(
        kind = selectorKind,
        type = nodeType,
        id = id,
    )
