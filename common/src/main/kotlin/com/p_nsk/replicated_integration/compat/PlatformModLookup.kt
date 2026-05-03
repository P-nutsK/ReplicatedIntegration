package com.p_nsk.replicated_integration.compat

fun interface PlatformModLookup {
    fun isLoaded(modId: String): Boolean
}
