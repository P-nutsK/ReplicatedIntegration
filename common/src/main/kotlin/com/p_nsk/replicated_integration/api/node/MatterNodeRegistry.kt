package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

@Suppress("unused")
class MatterNodeRegistry<C : MatterCommandDef> {
    private val nodes = linkedMapOf<LiteResourceLocation, MatterNodeDef<C>>()
    private val commandsByLiteral = linkedMapOf<String, C>()

    fun register(node: MatterNodeDef<C>) {
        val previous = nodes[node.id]
        require(previous == null || previous.withoutCommands() == node.withoutCommands()) {
            "Conflicting matter node registration for ${node.id}"
        }

        for (command in node.commands.all) {
            require(command.nodeType == node.id) {
                "Command ${command.literal} references ${command.nodeType}, but node is ${node.id}"
            }

            val previousCommand = commandsByLiteral[command.literal]
            require(previousCommand == null || previousCommand == command) {
                "Conflicting matter command literal: ${command.literal}"
            }

            commandsByLiteral[command.literal] = command
        }

        nodes[node.id] = node
    }

    fun get(id: LiteResourceLocation): MatterNodeDef<C>? =
        nodes[id]

    fun all(): List<MatterNodeDef<C>> =
        nodes.values.toList()

    fun commands(): List<C> =
        commandsByLiteral.values.toList()

    private fun MatterNodeDef<C>.withoutCommands(): MatterNodeDef<C> =
        copy(commands = MatterNodeCommands())
}
