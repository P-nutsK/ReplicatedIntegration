package com.p_nsk.replicated_integration.replication

import com.p_nsk.replicated_integration.Constants

object NoOpReplicationBridge : ReplicationBridge {
    override fun installBaseHooks() {
        Constants.LOGGER.debug("Replication bridge scaffold is active, but no concrete hooks are registered yet.")
    }
}
