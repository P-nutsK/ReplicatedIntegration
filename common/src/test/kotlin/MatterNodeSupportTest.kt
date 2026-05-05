package com.p_nsk.replicated_integration.api

import kotlin.test.Test
import kotlin.test.assertEquals

class MatterNodeSupportTest {
    @Test
    fun formatsRegisteredNodeType() {
        val registry = MatterNodeTypeRegistry.withDefaults()
        registry.register(
            MatterNodeTypeDef(
                id = LiteResourceLocation.of("replicated_integration", "essence"),
                displayName = "Essence",
                formatter = { id -> "essence:${id.path}" },
            )
        )

        val node = MatterNodeKey(
            LiteResourceLocation.of("replicated_integration", "essence"),
            LiteResourceLocation.of("example", "charged"),
        )

        assertEquals("essence:charged [Essence]", MatterNodeFormatter.formatNode(node, registry))
    }

    @Test
    fun fallsBackForUnknownNodeType() {
        val registry = MatterNodeTypeRegistry.withDefaults()
        val node = MatterNodeKey(
            LiteResourceLocation.of("unknown", "type"),
            LiteResourceLocation.of("minecraft", "stone"),
        )

        assertEquals("minecraft:stone [unknown:type]", MatterNodeFormatter.formatNode(node, registry))
    }
}
