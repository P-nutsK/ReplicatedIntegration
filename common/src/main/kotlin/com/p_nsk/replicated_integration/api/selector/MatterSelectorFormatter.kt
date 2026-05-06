package com.p_nsk.replicated_integration.api.selector

import com.p_nsk.replicated_integration.api.node.MatterNodeTypeRegistry

object MatterSelectorFormatter {
    fun format(selector: MatterSelectorKey, nodeTypes: MatterNodeTypeRegistry): String {
        val label = nodeTypes.get(selector.type)?.displayName ?: selector.type.toString()
        return when (selector.selector) {
            MatterSelectorKind.NODE -> "${selector.id} [$label]"
            MatterSelectorKind.TAG -> "#${selector.id} [$label Tag]"
        }
    }
}
