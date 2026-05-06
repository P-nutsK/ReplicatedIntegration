package com.p_nsk.replicated_integration.mekanism.synthetic

import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

object MekanismSyntheticConversionDefinitions {
    val nuclear: List<MekanismSyntheticChemicalConversionDefinition> =
        listOf(
            MekanismSyntheticChemicalConversionDefinition(
                id = LiteResourceLocation.of(Constants.MOD_ID, "mekanism/synthetic/fissile_fuel_to_nuclear_waste"),
                input = LiteResourceLocation.of("mekanism", "fissile_fuel"),
                inputAmount = 1L,
                output = LiteResourceLocation.of("mekanism", "nuclear_waste"),
                outputAmount = 1L,
            ),
            MekanismSyntheticChemicalConversionDefinition(
                id = LiteResourceLocation.of(Constants.MOD_ID, "mekanism/synthetic/polonium_to_antimatter"),
                input = LiteResourceLocation.of("mekanism", "polonium"),
                inputAmount = 1000L,
                output = LiteResourceLocation.of("mekanism", "antimatter"),
                outputAmount = 1L,
            ),
        )
}

data class MekanismSyntheticChemicalConversionDefinition(
    val id: LiteResourceLocation,
    val input: LiteResourceLocation,
    val inputAmount: Long,
    val output: LiteResourceLocation,
    val outputAmount: Long,
)
