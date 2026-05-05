package com.p_nsk.replicated_integration.mixin;

import com.buuz135.replication.calculation.MatterCompound;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.p_nsk.replicated_integration.addon.CalculationArtifacts;
import com.p_nsk.replicated_integration.addon.ForgeReplicationCalculationService;
import com.p_nsk.replicated_integration.Constants;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(ReplicationCalculation.class)
public abstract class ReplicationCalculationMixin {

    @Shadow(remap = false) public static HashMap<String, MatterCompound> DEFAULT_MATTER_COMPOUND;
    @Shadow(remap = false) private static CompoundTag cachedSyncTag;

    @Inject(method = "calculateRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replicatedIntegration$replaceCalculation(CallbackInfo ci) {
        ci.cancel();
        Thread thread = new Thread(() -> {
            Constants.INSTANCE.getLOGGER().info("Replacing Replication calculateRecipes with replicated_integration addon pipeline");
            CalculationArtifacts artifacts = ForgeReplicationCalculationService.INSTANCE.calculate();
            if (artifacts == null) {
                Constants.INSTANCE.getLOGGER().warn("Replication addon calculation returned no artifacts");
                return;
            }
            DEFAULT_MATTER_COMPOUND = artifacts.getCompounds();
            cachedSyncTag = artifacts.getSyncTag();
            Constants.INSTANCE.getLOGGER().info("Replication addon calculation applied {} exported compounds", DEFAULT_MATTER_COMPOUND.size());
            ForgeReplicationCalculationService.INSTANCE.syncToPlayers(cachedSyncTag);
        }, "Replication-Integration");
        thread.setDaemon(true);
        thread.start();
    }
}
