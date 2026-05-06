package com.p_nsk.replicated_integration.recipe

import com.buuz135.replication.recipe.MatterValueRecipe
import com.p_nsk.replicated_integration.api.recipe.MatterValueRecipeExtension

var MatterValueRecipe.replicatedIntegrationDenied: Boolean
    get() = (this as MatterValueRecipeExtension)
        .`replicated_integration$isDenied`()
    set(value) {
        (this as MatterValueRecipeExtension)
            .`replicated_integration$setDenied`(value)
    }
