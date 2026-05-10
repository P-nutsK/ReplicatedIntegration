package com.p_nsk.replicated_integration.api.node

import com.p_nsk.replicated_integration.api.model.LiteResourceLocation

@Suppress("unused")
object MatterNodes {
    @JvmField
    val ITEM = LiteResourceLocation.of("c", "item")

    @JvmField
    val FLUID = LiteResourceLocation.of("c", "fluid")

    @JvmField
    val CHEMICAL = LiteResourceLocation.of("mekanism", "chemical")

    @JvmField
    val GAS = LiteResourceLocation.of("mekanism", "gas")

    @JvmField
    val INFUSE_TYPE = LiteResourceLocation.of("mekanism", "infuse_type")

    @JvmField
    val PIGMENT = LiteResourceLocation.of("mekanism", "pigment")

    @JvmField
    val SLURRY = LiteResourceLocation.of("mekanism", "slurry")

    @JvmStatic
    fun item(id: LiteResourceLocation) = NodeKey(ITEM, id)

    @JvmStatic
    fun fluid(id: LiteResourceLocation) = NodeKey(FLUID, id)

    @JvmStatic
    fun chemical(id: LiteResourceLocation) = NodeKey(CHEMICAL, id)

    @JvmStatic
    fun gas(id: LiteResourceLocation) = NodeKey(GAS, id)

    @JvmStatic
    fun infuseType(id: LiteResourceLocation) = NodeKey(INFUSE_TYPE, id)

    @JvmStatic
    fun pigment(id: LiteResourceLocation) = NodeKey(PIGMENT, id)

    @JvmStatic
    fun slurry(id: LiteResourceLocation) = NodeKey(SLURRY, id)
}
