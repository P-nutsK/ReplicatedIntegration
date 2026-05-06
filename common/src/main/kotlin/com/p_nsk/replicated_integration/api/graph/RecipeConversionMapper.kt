package com.p_nsk.replicated_integration.api.graph

interface RecipeConversionMapper<R> {
    fun supports(recipe: Any): Boolean

    fun collect(recipe: R, collector: IConversionSink)
}
