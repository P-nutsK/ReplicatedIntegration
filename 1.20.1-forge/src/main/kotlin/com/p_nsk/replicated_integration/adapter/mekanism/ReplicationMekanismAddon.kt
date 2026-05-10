package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.adapter.mekanism.synthetic.ForgeMekanismSyntheticConversionContributor
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation
import com.p_nsk.replicated_integration.config.ForgeCompatibilityConfig
import com.p_nsk.replicated_integration.core.ForgeMatterNodeRegistry
import com.p_nsk.replicated_integration.core.ForgeReplicationAddon
import com.p_nsk.replicated_integration.core.ForgeReplicationAddonContext
import com.p_nsk.replicated_integration.core.node
import net.minecraft.world.item.crafting.Recipe

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ReplicationMekanismAddon : ForgeReplicationAddon {
    override val id: String = "mekanism"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean =
        environment.isModLoaded(id)

    @Suppress("UNCHECKED_CAST")
    override fun collectConversions(context: ForgeReplicationAddonContext, collector: IConversionSink) {
        for (recipe in context.recipeManager.recipes) {
            val mapper = MekanismRecipeMappers.all.firstOrNull { it.supports(recipe) } ?: continue
            mapper.collect(recipe as Recipe<*>, collector)
        }
        if (ForgeCompatibilityConfig.isNuclearRecipeEnabled()) {
            ForgeMekanismSyntheticConversionContributor.collectNuclearConversions(collector)
        }
    }

    override fun registerMatterNodes(registry: ForgeMatterNodeRegistry) = with(registry) {
        node(MekanismMatterNodes.GAS, "Gas") {
            value(
                literal = "gas",
                suggestions = MekanismMatterCommandInputs.gasSuggestions(),
                validate = MekanismMatterCommandInputs.gasValidator(),
            )

            tag(
                literal = "gas_tag",
                suggestions = MekanismMatterCommandInputs.gasTagSuggestions(),
                validate = MekanismMatterCommandInputs.gasTagValidator(),
            )
        }

        node(MekanismMatterNodes.INFUSE_TYPE, "Infuse Type") {
            value(
                literal = "infuse_type",
                suggestions = MekanismMatterCommandInputs.infuseTypeSuggestions(),
                validate = MekanismMatterCommandInputs.infuseTypeValidator(),
            )
        }

        node(MekanismMatterNodes.PIGMENT, "Pigment") {
            value(
                literal = "pigment",
                suggestions = MekanismMatterCommandInputs.pigmentSuggestions(),
                validate = MekanismMatterCommandInputs.pigmentValidator(),
            )
        }

        node(MekanismMatterNodes.SLURRY, "Slurry") {
            value(
                literal = "slurry",
                suggestions = MekanismMatterCommandInputs.slurrySuggestions(),
                validate = MekanismMatterCommandInputs.slurryValidator(),
            )
        }
    }

    object MekanismMatterNodes {
        val GAS = LiteResourceLocation("mekanism", "gas")
        val INFUSE_TYPE = LiteResourceLocation("mekanism", "infuse_type")
        val PIGMENT = LiteResourceLocation("mekanism", "pigment")
        val SLURRY = LiteResourceLocation("mekanism", "slurry")
    }

}
