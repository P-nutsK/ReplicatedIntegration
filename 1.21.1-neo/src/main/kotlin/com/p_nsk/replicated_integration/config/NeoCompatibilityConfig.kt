package com.p_nsk.replicated_integration.config

import net.neoforged.neoforge.common.ModConfigSpec

object NeoCompatibilityConfig {
    private const val KEY_ENABLE_NUCLEAR_RECIPE = "enableNuclearRecipe"

    val spec: ModConfigSpec
    private val enableNuclearRecipeValue: ModConfigSpec.BooleanValue

    init {
        val builder = ModConfigSpec.Builder()
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
