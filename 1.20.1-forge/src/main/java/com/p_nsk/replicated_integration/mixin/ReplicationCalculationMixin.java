package com.p_nsk.replicated_integration.mixin;

import com.buuz135.replication.calculation.MatterCompound;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.p_nsk.replicated_integration.bridge.CalculationArtifacts;
import com.p_nsk.replicated_integration.bridge.ForgeReplicationCalculationService;
import com.p_nsk.replicated_integration.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Mixin(ReplicationCalculation.class)
public abstract class ReplicationCalculationMixin {
    private static final ExecutorService CALCULATION_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Replication-Integration-Forge");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicLong CALCULATION_GENERATION = new AtomicLong();

    @Shadow(remap = false) public static HashMap<String, MatterCompound> DEFAULT_MATTER_COMPOUND;
    @Shadow(remap = false) private static CompoundTag cachedSyncTag;

    @Inject(method = "calculateRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replicatedIntegration$replaceCalculation(CallbackInfo ci) {
        ci.cancel();
        final long generation = CALCULATION_GENERATION.incrementAndGet();
        CALCULATION_EXECUTOR.execute(() -> {
            Constants.INSTANCE.getLOGGER().info("Replacing Replication calculateRecipes with replicated_integration addon pipeline");
            CalculationArtifacts artifacts = ForgeReplicationCalculationService.INSTANCE.calculate();
            if (artifacts == null) {
                Constants.INSTANCE.getLOGGER().warn("Replication addon calculation returned no artifacts");
                return;
            }
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Runnable applyArtifacts = () -> {
                if (generation != CALCULATION_GENERATION.get()) {
                    Constants.INSTANCE.getLOGGER().info("Skipping stale Forge calculation generation {}", generation);
                    return;
                }
                DEFAULT_MATTER_COMPOUND = artifacts.getCompounds();
                // Suppress Replication's built-in login sync path; we resend safely ourselves.
                cachedSyncTag = new CompoundTag();
                Constants.INSTANCE.getLOGGER().info("Replication addon calculation applied {} exported compounds", DEFAULT_MATTER_COMPOUND.size());
                ForgeReplicationCalculationService.INSTANCE.syncToPlayers(artifacts.getSyncTag());
            };
            if (server != null) {
                server.execute(applyArtifacts);
            } else {
                applyArtifacts.run();
            }
        });
    }
}
