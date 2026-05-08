package com.p_nsk.replicated_integration.core

import com.buuz135.replication.calculation.MatterCompound
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item

data class CalculationArtifacts(
    val syncTag: CompoundTag,
    val compounds: HashMap<Item, MatterCompound>,
)
