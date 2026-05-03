package com.p_nsk.replicated_integration.compat

interface CompatModule {
    val id: String
    val displayName: String
    val condition: LoadedModCondition

    fun initialize(context: CompatContext)
}
