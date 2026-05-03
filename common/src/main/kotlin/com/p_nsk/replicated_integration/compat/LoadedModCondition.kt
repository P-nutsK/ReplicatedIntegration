package com.p_nsk.replicated_integration.compat

data class LoadedModCondition(val targetModId: String) {
    fun matches(modLookup: PlatformModLookup): Boolean = modLookup.isLoaded(targetModId)
}
