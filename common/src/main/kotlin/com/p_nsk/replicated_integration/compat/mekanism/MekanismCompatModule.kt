package com.p_nsk.replicated_integration.compat.mekanism

import com.p_nsk.replicated_integration.Constants
import com.p_nsk.replicated_integration.compat.CompatContext
import com.p_nsk.replicated_integration.compat.CompatModule
import com.p_nsk.replicated_integration.compat.LoadedModCondition

object MekanismCompatModule : CompatModule {
    override val id: String = "mekanism"
    override val displayName: String = "Mekanism"
    override val condition: LoadedModCondition = LoadedModCondition(id)

    override fun initialize(context: CompatContext) {
        Constants.LOGGER.info(
            "Mekanism compat scaffold is active on {}. Replication hooks can be added here later.",
            context.platformName,
        )
        context.replicationBridge.installBaseHooks()
    }
}
