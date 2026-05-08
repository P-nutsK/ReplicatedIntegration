package com.p_nsk.replicated_integration.api

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterNodeFormatter
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.MatterNodeTypeDef
import com.p_nsk.replicated_integration.api.node.MatterNodeTypeRegistry
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

        val node = NodeKey(
            LiteResourceLocation.of("replicated_integration", "essence"),
            LiteResourceLocation.of("example", "charged"),
        )

        assertEquals("essence:charged [Essence]", MatterNodeFormatter.formatNode(node, registry))
    }

    @Test
    fun fallsBackForUnknownNodeType() {
        val registry = MatterNodeTypeRegistry.withDefaults()
        val node = NodeKey(
            LiteResourceLocation.of("unknown", "type"),
            LiteResourceLocation.of("minecraft", "stone"),
        )

        assertEquals("minecraft:stone [unknown:type]", MatterNodeFormatter.formatNode(node, registry))
    }
}
