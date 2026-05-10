package com.p_nsk.replicated_integration.api

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.api.node.MatterCommandDef
import com.p_nsk.replicated_integration.api.node.MatterNodeDef
import com.p_nsk.replicated_integration.api.node.MatterNodeRegistry
import com.p_nsk.replicated_integration.api.node.NodeKey
import com.p_nsk.replicated_integration.api.node.formatNode
import kotlin.test.Test
import kotlin.test.assertEquals

class MatterNodeSupportTest {
    @Test
    fun formatsRegisteredNodeType() {
        val registry = MatterNodeRegistry<TestCommand>()
        registry.register(
            MatterNodeDef(
                id = LiteResourceLocation.of("replicated_integration", "essence"),
                displayName = "Essence",
                formatter = { id -> "essence:${id.path}" },
            )
        )

        val node = NodeKey(
            LiteResourceLocation.of("replicated_integration", "essence"),
            LiteResourceLocation.of("example", "charged"),
        )

        assertEquals("essence:charged [Essence]", registry.formatNode(node))
    }

    @Test
    fun fallsBackForUnknownNodeType() {
        val registry = MatterNodeRegistry<TestCommand>()
        val node = NodeKey(
            LiteResourceLocation.of("unknown", "type"),
            LiteResourceLocation.of("minecraft", "stone"),
        )

        assertEquals("minecraft:stone [unknown:type]", registry.formatNode(node))
    }

    private data class TestCommand(
        override val literal: String,
        override val nodeType: LiteResourceLocation,
    ) : MatterCommandDef
}
