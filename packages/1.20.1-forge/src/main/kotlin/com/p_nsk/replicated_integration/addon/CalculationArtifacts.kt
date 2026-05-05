package com.p_nsk.replicated_integration.addon

import com.buuz135.replication.calculation.MatterCompound
import net.minecraft.nbt.CompoundTag

data class CalculationArtifacts(
    val syncTag: CompoundTag,
    val compounds: HashMap<String, MatterCompound>,
)
