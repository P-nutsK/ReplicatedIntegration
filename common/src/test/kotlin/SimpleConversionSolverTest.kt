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
            oakLog to ExplicitMatterValue.Set(LiteMatterCompound.single(wood, 32.0), ExplicitMatterSource.DATAPACK),
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
                explicitValues =
                    mapOf(
                        water to ExplicitMatterValue.Set(LiteMatterCompound.single(earth, 10.0), ExplicitMatterSource.DATAPACK),
                        oxygen to ExplicitMatterValue.Set(LiteMatterCompound.single(earth, 6.0), ExplicitMatterSource.DATAPACK),
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
                explicitValues =
                    mapOf(
                        steam to ExplicitMatterValue.Set(LiteMatterCompound.single(matter, 10.0), ExplicitMatterSource.DATAPACK),
                    ),
            )

        assertEquals(10.0, solved[steam]!!.values[matter])
        assertEquals(10.0, solved[water]!!.values[matter])
        assertEquals(10.0, solved[metallicSteam]!!.values[matter])
    }

    @Test
    fun selectorMaterializerExpandsTagSelectorsIntoConcreteNodes() {
        val oakLog = MatterNodes.item(LiteResourceLocation.of("minecraft", "oak_log"))
        val oakWood = MatterNodes.item(LiteResourceLocation.of("minecraft", "oak_wood"))
        val earth = LiteResourceLocation.of("replication", "earth")
        val logsSelector = MatterSelectorKey(MatterSelectorKind.TAG, MatterNodes.ITEM, LiteResourceLocation.of("minecraft", "oak_logs"))

        val materialized =
            MatterSelectorMaterializer.materialize(
                selectors =
                    mapOf(
                        logsSelector to ExplicitMatterValue.Set(LiteMatterCompound.single(earth, 20000.0), ExplicitMatterSource.DATAPACK),
                    ),
                expandTag = { type, id ->
                    if (type == MatterNodes.ITEM && id == LiteResourceLocation.of("minecraft", "oak_logs")) {
                        listOf(oakLog, oakWood)
                    } else {
                        emptyList()
                    }
                },
            )

        assertEquals(20000.0, (materialized[oakLog] as ExplicitMatterValue.Set).compound.values[earth])
        assertEquals(20000.0, (materialized[oakWood] as ExplicitMatterValue.Set).compound.values[earth])
    }

    @Test
    fun selectorMaterializerPrefersConcreteSelectorsOverTagsAtSameSource() {
        val oakLog = MatterNodes.item(LiteResourceLocation.of("minecraft", "oak_log"))
        val earth = LiteResourceLocation.of("replication", "earth")

        val materialized =
            MatterSelectorMaterializer.materialize(
                selectors =
                    mapOf(
                        MatterSelectorKey(MatterSelectorKind.TAG, MatterNodes.ITEM, LiteResourceLocation.of("minecraft", "oak_logs")) to
                            ExplicitMatterValue.Deny(ExplicitMatterSource.RUNTIME),
                        MatterSelectorKey(MatterSelectorKind.NODE, MatterNodes.ITEM, LiteResourceLocation.of("minecraft", "oak_log")) to
                            ExplicitMatterValue.Set(LiteMatterCompound.single(earth, 20.0), ExplicitMatterSource.RUNTIME),
                    ),
                expandTag = { _, _ -> listOf(oakLog) },
            )

        assertEquals(20.0, (materialized[oakLog] as ExplicitMatterValue.Set).compound.values[earth])
    }
}
