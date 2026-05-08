package com.p_nsk.replicated_integration.api.addon

@RequiresOptIn(
    message = "Implementing ReplicationAddon opts into the load-safety contract. " +
            "Addon classes/objects may be loaded before isEnabled() is checked; " +
            "optional dependency classes must not be referenced during class/object loading.",
    level = RequiresOptIn.Level.WARNING,
)
annotation class ReplicationAddonLoadSafetyContract
