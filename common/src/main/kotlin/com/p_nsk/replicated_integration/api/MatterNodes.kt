package com.p_nsk.replicated_integration.api
@Suppress("unused")
object MatterNodes {
    @JvmField
    val ITEM = LiteResourceLocation.of("c", "item")
    @JvmField
    val ITEM_TAG = LiteResourceLocation.of("c", "item_tag")
    @JvmField
    val FLUID = LiteResourceLocation.of("c", "fluid")
    @JvmField
    val FLUID_TAG = LiteResourceLocation.of("c", "fluid_tag")
    @JvmField
    val CHEMICAL = LiteResourceLocation.of("mekanism", "chemical")
    @JvmField
    val CHEMICAL_TAG = LiteResourceLocation.of("mekanism", "chemical_tag")
    @JvmField
    val GAS = LiteResourceLocation.of("mekanism", "gas")
    @JvmField
    val GAS_TAG = LiteResourceLocation.of("mekanism", "gas_tag")
    @JvmField
    val INFUSE_TYPE = LiteResourceLocation.of("mekanism", "infuse_type")
    @JvmField
    val INFUSE_TYPE_TAG = LiteResourceLocation.of("mekanism", "infuse_type_tag")
    @JvmField
    val PIGMENT = LiteResourceLocation.of("mekanism", "pigment")
    @JvmField
    val PIGMENT_TAG = LiteResourceLocation.of("mekanism", "pigment_tag")
    @JvmField
    val SLURRY = LiteResourceLocation.of("mekanism", "slurry")
    @JvmField
    val SLURRY_TAG = LiteResourceLocation.of("mekanism", "slurry_tag")

    @JvmStatic
    fun item(id: LiteResourceLocation) = MatterNodeKey(ITEM, id)

    @JvmStatic
    fun itemTag(id: LiteResourceLocation) = MatterNodeKey(ITEM_TAG, id)

    @JvmStatic
    fun fluid(id: LiteResourceLocation) = MatterNodeKey(FLUID, id)

    @JvmStatic
    fun fluidTag(id: LiteResourceLocation) = MatterNodeKey(FLUID_TAG, id)

    @JvmStatic
    fun chemical(id: LiteResourceLocation) = MatterNodeKey(CHEMICAL, id)

    @JvmStatic
    fun chemicalTag(id: LiteResourceLocation) = MatterNodeKey(CHEMICAL_TAG, id)

    @JvmStatic
    fun gas(id: LiteResourceLocation) = MatterNodeKey(GAS, id)

    @JvmStatic
    fun gasTag(id: LiteResourceLocation) = MatterNodeKey(GAS_TAG, id)

    @JvmStatic
    fun infuseType(id: LiteResourceLocation) = MatterNodeKey(INFUSE_TYPE, id)

    @JvmStatic
    fun infuseTypeTag(id: LiteResourceLocation) = MatterNodeKey(INFUSE_TYPE_TAG, id)

    @JvmStatic
    fun pigment(id: LiteResourceLocation) = MatterNodeKey(PIGMENT, id)

    @JvmStatic
    fun pigmentTag(id: LiteResourceLocation) = MatterNodeKey(PIGMENT_TAG, id)

    @JvmStatic
    fun slurry(id: LiteResourceLocation) = MatterNodeKey(SLURRY, id)

    @JvmStatic
    fun slurryTag(id: LiteResourceLocation) = MatterNodeKey(SLURRY_TAG, id)

    @JvmStatic
    fun builtinTypes(): List<MatterNodeTypeDef> =
        listOf(
            MatterNodeTypeDef(ITEM, "Item"),
            MatterNodeTypeDef(ITEM_TAG, "Item Tag"),
            MatterNodeTypeDef(FLUID, "Fluid"),
            MatterNodeTypeDef(FLUID_TAG, "Fluid Tag"),
            MatterNodeTypeDef(CHEMICAL, "Chemical"),
            MatterNodeTypeDef(CHEMICAL_TAG, "Chemical Tag"),
            MatterNodeTypeDef(GAS, "Gas"),
            MatterNodeTypeDef(GAS_TAG, "Gas Tag"),
            MatterNodeTypeDef(INFUSE_TYPE, "Infuse Type"),
            MatterNodeTypeDef(INFUSE_TYPE_TAG, "Infuse Type Tag"),
            MatterNodeTypeDef(PIGMENT, "Pigment"),
            MatterNodeTypeDef(PIGMENT_TAG, "Pigment Tag"),
            MatterNodeTypeDef(SLURRY, "Slurry"),
            MatterNodeTypeDef(SLURRY_TAG, "Slurry Tag"),
        )
}
