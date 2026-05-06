package com.p_nsk.replicated_integration.api.graph

import com.p_nsk.replicated_integration.api.model.MatterConversion

interface IConversionSink {
    fun add(conversion: MatterConversion)
}
