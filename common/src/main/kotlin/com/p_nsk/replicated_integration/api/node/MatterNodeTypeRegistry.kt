package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

@Suppress("unused")
class MatterNodeTypeRegistry {
    private val types = linkedMapOf<LiteResourceLocation, MatterNodeTypeDef>()

    fun register(type: MatterNodeTypeDef) {
        val previous = types[type.id]
        require(previous == null || previous == type) {
            "Conflicting matter node type registration for ${type.id}"
        }
        types[type.id] = type
    }

    fun registerAll(types: Iterable<MatterNodeTypeDef>) {
        types.forEach(::register)
    }

    fun get(type: LiteResourceLocation): MatterNodeTypeDef? = types[type]

    fun all(): List<MatterNodeTypeDef> = types.values.toList()

    companion object {
        @JvmStatic
        fun withDefaults(): MatterNodeTypeRegistry =
            MatterNodeTypeRegistry().apply {
                registerAll(MatterNodes.builtinTypes())
            }
    }
}
