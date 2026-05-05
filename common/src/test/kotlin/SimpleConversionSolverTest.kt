package com.p_nsk.replicated_integration.api

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleConversionSolverTest {
    @Test
    fun solvesSimpleWoodChain() {
        val oakLog = MatterNodes.item(LiteResourceLocation.of("minecraft", "oak_log"))
        val oakPlanks = MatterNodes.item(LiteResourceLocation.of("minecraft", "oak_planks"))
        val stick = MatterNodes.item(LiteResourceLocation.of("minecraft", "stick"))

        val builder = ConversionGraphBuilder()

        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("minecraft", "oak_planks"),
                consumes = listOf(MatterAmount(oakLog, 1)),
                produces = listOf(MatterAmount(oakPlanks, 4)),
            )
        )

        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("minecraft", "stick"),
                consumes = listOf(MatterAmount(oakPlanks, 2)),
                produces = listOf(MatterAmount(stick, 4)),
            )
        )

        val graph = builder.build()

        val wood = LiteResourceLocation.of("replication", "wood")

        val defaults = mapOf(
            oakLog to LiteMatterCompound.single(wood, 32.0),
        )

        val solved = SimpleConversionSolver().solve(graph, defaults)

        assertEquals(32.0, solved[oakLog]!!.values[wood])
        assertEquals(8.0, solved[oakPlanks]!!.values[wood])
        assertEquals(4.0, solved[stick]!!.values[wood])
    }
}
