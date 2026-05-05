package com.p_nsk.replicated_integration.mixin;

import com.buuz135.replication.recipe.MatterValueRecipe;
import com.p_nsk.replicated_integration.api.MatterValueRecipeExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MatterValueRecipe.class, remap = false)
public abstract class MatterValueRecipeMixin  implements MatterValueRecipeExtension {
    @Unique
    private boolean replicatedIntegration$deny;

    @Override
    public boolean replicated_integration$isDenied() {
        return replicatedIntegration$deny;
    }
    @Override
    public void replicated_integration$setDenied(boolean denied) {
        this.replicatedIntegration$deny = denied;
    }
}
