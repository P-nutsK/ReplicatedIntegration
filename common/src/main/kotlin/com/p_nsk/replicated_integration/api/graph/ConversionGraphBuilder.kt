package com.p_nsk.replicated_integration.api.graph

import com.p_nsk.replicated_integration.api.model.MatterConversion

class ConversionGraphBuilder : IConversionSink {
    private val conversions = mutableListOf<MatterConversion>()

    override fun add(conversion: MatterConversion) {
        conversions += conversion
    }

    fun build(): ConversionGraph =
        ConversionGraph(conversions.toList())
}
