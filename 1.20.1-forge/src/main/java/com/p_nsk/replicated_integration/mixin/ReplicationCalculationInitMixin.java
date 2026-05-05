package com.p_nsk.replicated_integration.mixin;

import com.buuz135.replication.calculation.ReplicationCalculation;
import com.p_nsk.replicated_integration.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReplicationCalculation.class, remap = false)
public abstract class ReplicationCalculationInitMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private static void replicatedIntegration$replaceInit(CallbackInfo ci) {
        Constants.INSTANCE.getLOGGER().info("Suppressing ReplicationCalculation.init(); replicated_integration will register calculation lifecycle hooks.");
        ci.cancel();
    }
}
