package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

@Suppress("unused")
object MatterNodeFormatter {
    @JvmStatic
    fun formatType(type: LiteResourceLocation, registry: MatterNodeTypeRegistry): String =
        registry.get(type)?.displayName ?: type.toString()

    @JvmStatic
    fun formatNode(node: MatterNodeKey, registry: MatterNodeTypeRegistry): String {
        val type = registry.get(node.type)
        val formattedId = type?.formatter?.invoke(node.id) ?: node.id.toString()
        return "$formattedId [${type?.displayName ?: node.type}]"
    }
}
