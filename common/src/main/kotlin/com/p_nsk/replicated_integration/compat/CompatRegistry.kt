package com.p_nsk.replicated_integration.compat

import com.p_nsk.replicated_integration.Constants

class CompatRegistry {
    private val modules = linkedMapOf<String, CompatModule>()

    fun register(module: CompatModule) {
        modules[module.id] = module
    }

    fun initialize(context: CompatContext) {
        modules.values.forEach { module ->
            if (!module.condition.matches(context.modLookup)) {
                Constants.LOGGER.debug("Skipping compat {} because {} is not loaded", module.id, module.condition.targetModId)
                return@forEach
            }

            Constants.LOGGER.info("Enabling compat {} for {}", module.id, module.displayName)
            module.initialize(context)
        }
    }
}
