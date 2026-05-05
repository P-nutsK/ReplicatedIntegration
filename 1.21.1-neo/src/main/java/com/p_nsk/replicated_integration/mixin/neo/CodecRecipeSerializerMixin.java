package com.p_nsk.replicated_integration.mixin.neo;

import com.buuz135.replication.recipe.MatterValueRecipe;
import com.hrznstudio.titanium.recipe.serializer.CodecRecipeSerializer;
import com.mojang.serialization.MapCodec;
import com.p_nsk.replicated_integration.recipe.NeoMatterValueRecipeCodec;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CodecRecipeSerializer.class, remap = false)
public abstract class CodecRecipeSerializerMixin<T extends Recipe<?>> {
    @Shadow
    @Final
    private Class<T> recipeClass;

    @Shadow
    @Final
    @Mutable
    private MapCodec<T> codec;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replicatedIntegration$replaceMatterValueCodec(CallbackInfo ci) {
        if (recipeClass == MatterValueRecipe.class) {
            this.codec = (MapCodec<T>) NeoMatterValueRecipeCodec.INSTANCE.getCODEC();
        }
    }
}
