package com.p_nsk.replicated_integration.api.selector

import com.p_nsk.replicated_integration.api.node.MatterNodeRegistry
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.formatNode
import com.p_nsk.replicated_integration.api.node.formatType

fun MatterNodeRegistry<*>.formatSelector(selector: MatterSelectorKey): String =
    when (selector.kind) {
        MatterSelectorKind.NODE ->
            formatNode(NodeKey(selector.type, selector.id))

        MatterSelectorKind.TAG ->
            "#${selector.id} [${formatType(selector.type)} tag]"
    }
