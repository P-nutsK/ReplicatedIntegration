package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

fun MatterNodeRegistry<*>.formatType(type: LiteResourceLocation): String =
    get(type)?.displayName ?: type.toString()

fun MatterNodeRegistry<*>.formatNode(node: NodeKey): String {
    val def = get(node.type)
    val formattedId = def?.formatter?.invoke(node.id) ?: node.id.toString()
    return "$formattedId [${def?.displayName ?: node.type}]"
}
