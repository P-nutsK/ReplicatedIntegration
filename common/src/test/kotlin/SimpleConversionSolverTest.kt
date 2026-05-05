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
                produces = MatterAmount(oakPlanks, 4),
            )
        )

        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("minecraft", "stick"),
                consumes = listOf(MatterAmount(oakPlanks, 2)),
                produces = MatterAmount(stick, 4),
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

    @Test
    fun subtractsKnownCreditsBeforeDividing() {
        val water = MatterNodes.fluid(LiteResourceLocation.of("minecraft", "water"))
        val hydrogen = MatterNodes.gas(LiteResourceLocation.of("mekanism", "hydrogen"))
        val oxygen = MatterNodes.gas(LiteResourceLocation.of("mekanism", "oxygen"))
        val earth = LiteResourceLocation.of("replication", "earth")

        val builder = ConversionGraphBuilder()
        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("mekanism", "electrolysis/water_to_hydrogen"),
                consumes = listOf(MatterAmount(water, 2)),
                produces = MatterAmount(hydrogen, 1),
                credits = listOf(MatterAmount(oxygen, 1)),
            )
        )

        val solved =
            SimpleConversionSolver().solve(
                graph = builder.build(),
                defaults =
                    mapOf(
                        water to LiteMatterCompound.single(earth, 10.0),
                        oxygen to LiteMatterCompound.single(earth, 6.0),
                    ),
            )

        assertEquals(14.0, solved[hydrogen]!!.values[earth])
    }

    @Test
    fun doesNotReenterSameLoopGuardChain() {
        val water = MatterNodes.fluid(LiteResourceLocation.of("minecraft", "water"))
        val steam = MatterNodes.gas(LiteResourceLocation.of("mekanism", "steam"))
        val metallicSteam = MatterNodes.gas(LiteResourceLocation.of("test", "metallic_steam"))
        val matter = LiteResourceLocation.of("replication", "metallic")
        val rotaryLoop = LiteResourceLocation.of("mekanism", "water_steam_rotary")

        val builder = ConversionGraphBuilder()
        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("mekanism", "rotary/water_to_steam"),
                consumes = listOf(MatterAmount(water, 1)),
                produces = MatterAmount(steam, 2),
                loopGuardKey = rotaryLoop,
            )
        )
        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("mekanism", "rotary/steam_to_water"),
                consumes = listOf(MatterAmount(steam, 1)),
                produces = MatterAmount(water, 1),
                loopGuardKey = rotaryLoop,
            )
        )
        builder.add(
            MatterConversion(
                id = LiteResourceLocation.of("test", "steam_polish"),
                consumes = listOf(MatterAmount(steam, 1)),
                produces = MatterAmount(metallicSteam, 1),
            )
        )

        val solved =
            SimpleConversionSolver().solve(
                graph = builder.build(),
                defaults =
                    mapOf(
                        steam to LiteMatterCompound.single(matter, 10.0),
                    ),
            )

        assertEquals(10.0, solved[steam]!!.values[matter])
        assertEquals(10.0, solved[water]!!.values[matter])
        assertEquals(10.0, solved[metallicSteam]!!.values[matter])
    }
}
