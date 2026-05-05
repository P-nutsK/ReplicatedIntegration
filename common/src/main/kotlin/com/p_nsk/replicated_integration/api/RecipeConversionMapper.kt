package com.p_nsk.replicated_integration.api

interface RecipeConversionMapper<R> {
    fun supports(recipe: Any): Boolean

    fun collect(recipe: R, collector: IConversionSink)
}
