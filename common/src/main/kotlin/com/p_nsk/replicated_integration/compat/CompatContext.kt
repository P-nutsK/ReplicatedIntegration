package com.p_nsk.replicated_integration.compat

import com.p_nsk.replicated_integration.replication.ReplicationBridge

data class CompatContext(
    val platformName: String,
    val modLookup: PlatformModLookup,
    val replicationBridge: ReplicationBridge,
)
