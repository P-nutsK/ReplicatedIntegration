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
    fun item(id: LiteResourceLocation) = MatterNodeKey(ITEM, id)

    @JvmStatic
    fun fluid(id: LiteResourceLocation) = MatterNodeKey(FLUID, id)

    @JvmStatic
    fun chemical(id: LiteResourceLocation) = MatterNodeKey(CHEMICAL, id)

    @JvmStatic
    fun gas(id: LiteResourceLocation) = MatterNodeKey(GAS, id)

    @JvmStatic
    fun infuseType(id: LiteResourceLocation) = MatterNodeKey(INFUSE_TYPE, id)

    @JvmStatic
    fun pigment(id: LiteResourceLocation) = MatterNodeKey(PIGMENT, id)

    @JvmStatic
    fun slurry(id: LiteResourceLocation) = MatterNodeKey(SLURRY, id)

    @JvmStatic
    fun builtinTypes(): List<MatterNodeTypeDef> =
        listOf(
            MatterNodeTypeDef(ITEM, "Item"),
            MatterNodeTypeDef(FLUID, "Fluid"),
            MatterNodeTypeDef(CHEMICAL, "Chemical"),
            MatterNodeTypeDef(GAS, "Gas"),
            MatterNodeTypeDef(INFUSE_TYPE, "Infuse Type"),
            MatterNodeTypeDef(PIGMENT, "Pigment"),
            MatterNodeTypeDef(SLURRY, "Slurry"),
        )
}
