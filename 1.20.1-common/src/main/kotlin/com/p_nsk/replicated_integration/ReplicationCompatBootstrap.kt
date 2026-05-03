package com.p_nsk.replicated_integration

import com.p_nsk.replicated_integration.compat.PlatformModLookup
import com.p_nsk.replicated_integration.compat.mekanism.MekanismCompatModule

object ReplicationCompatBootstrap {
    fun create(modLookup: PlatformModLookup): ReplicatedIntegration =
        ReplicatedIntegration.create(
            platformName = "1.20.1-forge",
            modLookup = modLookup,
            modules = listOf(MekanismCompatModule),
        )
}
