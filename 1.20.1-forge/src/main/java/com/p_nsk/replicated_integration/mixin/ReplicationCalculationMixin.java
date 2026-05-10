package com.p_nsk.replicated_integration.mixin;

import com.buuz135.replication.calculation.MatterCompound;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.p_nsk.replicated_integration.core.CalculationArtifacts;
import com.p_nsk.replicated_integration.core.ForgeCalculationSnapshot;
import com.p_nsk.replicated_integration.core.ForgeReplicationCalculationService;
import com.p_nsk.replicated_integration.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

@Mixin(value = ReplicationCalculation.class, remap = false)
public abstract class ReplicationCalculationMixin {
    @Unique
    private static final ExecutorService CALCULATION_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Replication-Integration-Forge");
        thread.setDaemon(true);
        return thread;
    });
    @Unique
    private static final Object CALCULATION_LOCK = new Object();
    @Unique
    private static final AtomicLong CALCULATION_GENERATION = new AtomicLong();
    @Unique
    @Nullable
    private static PendingCalculation PENDING_CALCULATION;
    @Unique
    private static boolean WORKER_RUNNING;

    @Shadow(remap = false) public static HashMap<String, MatterCompound> DEFAULT_MATTER_COMPOUND;
    @Shadow(remap = false) private static CompoundTag cachedSyncTag;

    @Inject(method = "calculateRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replicatedIntegration$replaceCalculation(CallbackInfo ci) {
        ci.cancel();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            Constants.INSTANCE.getLOGGER().info("Skipping replicated_integration calculation until a server is available");
            return;
        }
        ForgeCalculationSnapshot snapshot = ForgeReplicationCalculationService.INSTANCE.prepareSnapshot(server);
        synchronized (CALCULATION_LOCK) {
            long generation = CALCULATION_GENERATION.incrementAndGet();
            PENDING_CALCULATION = new PendingCalculation(generation, server, snapshot);
            if (WORKER_RUNNING) {
                return;
            }
            WORKER_RUNNING = true;
        }
        CALCULATION_EXECUTOR.execute(ReplicationCalculationMixin::replicatedIntegration$processPendingCalculations);
    }

    @Unique
    private static void replicatedIntegration$processPendingCalculations() {
        while (true) {
            PendingCalculation calculation;
            synchronized (CALCULATION_LOCK) {
                calculation = PENDING_CALCULATION;
                PENDING_CALCULATION = null;
                if (calculation == null) {
                    WORKER_RUNNING = false;
                    return;
                }
            }
            Constants.INSTANCE.getLOGGER().info("Replacing Replication calculateRecipes with replicated_integration addon pipeline");
            CalculationArtifacts artifacts = ForgeReplicationCalculationService.INSTANCE.calculate(calculation.snapshot());
            Runnable applyArtifacts = () -> {
                if (calculation.generation() != CALCULATION_GENERATION.get()) {
                    Constants.INSTANCE.getLOGGER().info("Skipping stale Forge calculation generation {}", calculation.generation());
                    return;
                }
                DEFAULT_MATTER_COMPOUND = artifacts.getCompounds();
                // Suppress Replication's built-in login sync path; we resend safely ourselves.
                cachedSyncTag = new CompoundTag();
                Constants.INSTANCE.getLOGGER().info("Replication addon calculation applied {} exported compounds", DEFAULT_MATTER_COMPOUND.size());
                ForgeReplicationCalculationService.INSTANCE.syncToPlayers(artifacts.getSyncTag());
            };
            calculation.server().execute(applyArtifacts);
        }
    }

    private record PendingCalculation(long generation, MinecraftServer server, ForgeCalculationSnapshot snapshot) {
    }
}
