package com.p_nsk.replicated_integration.mixin.neo;

import com.buuz135.replication.calculation.MatterCompound;
import com.buuz135.replication.calculation.ReplicationCalculation;
import com.buuz135.replication.api.MatterCalculationStatus;
import com.p_nsk.replicated_integration.Constants;
import com.p_nsk.replicated_integration.bridge.CalculationArtifacts;
import com.p_nsk.replicated_integration.bridge.NeoReplicationCalculationService;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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
        Thread thread = new Thread(runnable, "Replication-Integration-Neo");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicLong CALCULATION_GENERATION = new AtomicLong();

    @Shadow(remap = false)
    public static HashMap<Item, MatterCompound> DEFAULT_MATTER_COMPOUND;

    @Shadow(remap = false)
    private static CompoundTag cachedSyncTag;

    @Shadow(remap = false)
    public static MatterCalculationStatus STATUS;

    @Inject(method = "calculateRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replicatedIntegration$replaceCalculation(RegistryAccess registryAccess, CallbackInfo ci) {
        ci.cancel();
        final var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            Constants.INSTANCE.getLOGGER().info("Skipping replicated_integration calculation until a server is available");
            return;
        }
        STATUS = MatterCalculationStatus.NOT_CALCULATED;
        final long generation = CALCULATION_GENERATION.incrementAndGet();
        CALCULATION_EXECUTOR.execute(() -> {
            Constants.INSTANCE.getLOGGER().info("Replacing Replication calculateRecipes with replicated_integration addon pipeline");
            CalculationArtifacts artifacts = NeoReplicationCalculationService.INSTANCE.calculate();
            if (artifacts == null) {
                Constants.INSTANCE.getLOGGER().warn("Replication addon calculation returned no artifacts");
                return;
            }
            server.execute(() -> {
                if (generation != CALCULATION_GENERATION.get()) {
                    Constants.INSTANCE.getLOGGER().info("Skipping stale Neo calculation generation {}", generation);
                    return;
                }
                DEFAULT_MATTER_COMPOUND = artifacts.getCompounds();
                cachedSyncTag = artifacts.getSyncTag();
                STATUS = MatterCalculationStatus.CALCULATED;
                Constants.INSTANCE.getLOGGER().info("Replication addon calculation applied {} exported compounds", DEFAULT_MATTER_COMPOUND.size());
                NeoReplicationCalculationService.INSTANCE.syncToPlayers(cachedSyncTag);
            });
        });
    }
}
