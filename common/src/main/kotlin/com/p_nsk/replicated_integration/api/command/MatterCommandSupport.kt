package com.p_nsk.replicated_integration.api.command

import com.p_nsk.replicated_integration.api.model.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.model.LiteMatterCompound
import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

object MatterCommandSupport {
    val EARTH = LiteResourceLocation.of("replication", "earth")
    val NETHER = LiteResourceLocation.of("replication", "nether")
    val ORGANIC = LiteResourceLocation.of("replication", "organic")
    val ENDER = LiteResourceLocation.of("replication", "ender")
    val METALLIC = LiteResourceLocation.of("replication", "metallic")
    val PRECIOUS = LiteResourceLocation.of("replication", "precious")
    val LIVING = LiteResourceLocation.of("replication", "living")
    val QUANTUM = LiteResourceLocation.of("replication", "quantum")

    val allMatterTypes: List<Pair<String, LiteResourceLocation>> =
        listOf(
            "earth" to EARTH,
            "nether" to NETHER,
            "organic" to ORGANIC,
            "ender" to ENDER,
            "metallic" to METALLIC,
            "precious" to PRECIOUS,
            "living" to LIVING,
            "quantum" to QUANTUM,
        )

    fun singleMatterType(name: String): LiteResourceLocation? =
        allMatterTypes.firstOrNull { it.first == name }?.second

    fun sourceLabel(
        explicitValue: ExplicitMatterValue?,
        solvedValue: LiteMatterCompound?,
    ): String =
        when {
            explicitValue is ExplicitMatterValue.Deny -> "denied (${explicitValue.source.displayName})"
            explicitValue is ExplicitMatterValue.Set -> explicitValue.source.displayName
            solvedValue != null -> "recipe-derived"
            else -> "unresolved"
        }

    fun formatAmount(value: Double): String =
        if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.6f".format(value).trimEnd('0').trimEnd('.')
        }
}
