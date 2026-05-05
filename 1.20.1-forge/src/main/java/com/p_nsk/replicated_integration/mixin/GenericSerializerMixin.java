package com.p_nsk.replicated_integration.mixin;

import com.buuz135.replication.recipe.MatterValueRecipe;
import com.google.gson.JsonObject;
import com.hrznstudio.titanium.recipe.serializer.GenericSerializer;
import com.hrznstudio.titanium.recipe.serializer.SerializableRecipe;
import com.p_nsk.replicated_integration.recipe.ForgeMatterValueRecipeSerialization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(value = GenericSerializer.class, remap = false)
public abstract class GenericSerializerMixin<T extends SerializableRecipe> {
    @Shadow
    @Final
    private Class<T> recipeClass;

    @Shadow
    @Final
    private Supplier<RecipeType<?>> recipeTypeSupplier;

    @Inject(method = "fromJson", at = @At("HEAD"), cancellable = true)
    private void replicatedIntegration$readMatterValueJson(ResourceLocation id, JsonObject json, CallbackInfoReturnable<T> cir) {
        if (recipeClass == MatterValueRecipe.class) {
            cir.setReturnValue((T) ForgeMatterValueRecipeSerialization.INSTANCE.fromJson(id, json));
        }
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void replicatedIntegration$writeMatterValueJson(SerializableRecipe recipe, CallbackInfoReturnable<JsonObject> cir) {
        if (recipeClass == MatterValueRecipe.class && recipe instanceof MatterValueRecipe matterValueRecipe) {
            cir.setReturnValue(ForgeMatterValueRecipeSerialization.INSTANCE.write(matterValueRecipe, recipeTypeSupplier.get().toString()));
        }
    }

    @Inject(method = "fromNetwork", at = @At("HEAD"), cancellable = true)
    private void replicatedIntegration$readMatterValueNetwork(ResourceLocation id, FriendlyByteBuf buf, CallbackInfoReturnable<T> cir) {
        if (recipeClass == MatterValueRecipe.class) {
            cir.setReturnValue((T) ForgeMatterValueRecipeSerialization.INSTANCE.fromNetwork(id, buf));
        }
    }

    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private void replicatedIntegration$writeMatterValueNetwork(FriendlyByteBuf buf, SerializableRecipe recipe, CallbackInfo ci) {
        if (recipeClass == MatterValueRecipe.class && recipe instanceof MatterValueRecipe matterValueRecipe) {
            ForgeMatterValueRecipeSerialization.INSTANCE.toNetwork(buf, matterValueRecipe);
            ci.cancel();
        }
    }
}
