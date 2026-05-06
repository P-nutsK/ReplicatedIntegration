package com.p_nsk.replicated_integration.config

import net.minecraftforge.common.ForgeConfigSpec

object ForgeCompatibilityConfig {
    private const val KEY_ENABLE_NUCLEAR_RECIPE = "enableNuclearRecipe"

    val spec: ForgeConfigSpec
    private val enableNuclearRecipeValue: ForgeConfigSpec.BooleanValue

    init {
        val builder = ForgeConfigSpec.Builder()
        builder.push("mekanism")
        enableNuclearRecipeValue =
            builder
                .comment("Enable synthetic Mekanism nuclear conversions such as fissile fuel -> nuclear waste and polonium -> antimatter.")
                .translation("replicated_integration.config.enable_nuclear_recipe")
                .define(KEY_ENABLE_NUCLEAR_RECIPE, false)
        builder.pop()
        spec = builder.build()
    }

    fun isNuclearRecipeEnabled(): Boolean = enableNuclearRecipeValue.get()
}
