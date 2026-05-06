package com.p_nsk.replicated_integration.api.recipe

@Suppress("FunctionName")
interface MatterValueRecipeExtension {
    fun `replicated_integration$isDenied`(): Boolean

    fun `replicated_integration$setDenied`(denied: Boolean)
}
