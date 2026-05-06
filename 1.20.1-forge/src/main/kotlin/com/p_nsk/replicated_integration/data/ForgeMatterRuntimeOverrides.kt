package com.p_nsk.replicated_integration.data

import com.p_nsk.replicated_integration.api.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.LiteMatterCompound
import com.p_nsk.replicated_integration.api.MatterSelectorKey
import com.p_nsk.replicated_integration.api.MatterSelectorStorageCodec
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path

object ForgeMatterRuntimeOverrides {
    private var loadedFrom: Path? = null
    private var values: Map<MatterSelectorKey, ExplicitMatterValue> = emptyMap()

    @Synchronized
    fun snapshot(server: MinecraftServer): Map<MatterSelectorKey, ExplicitMatterValue> {
        loadIfNeeded(server)
        return values
    }

    @Synchronized
    fun set(
        server: MinecraftServer,
        selector: MatterSelectorKey,
        compound: LiteMatterCompound,
    ) {
        loadIfNeeded(server)
        values = values.toMutableMap().also { it[selector] = ExplicitMatterValue.Set(compound, ExplicitMatterSource.RUNTIME) }
        save(server)
    }

    @Synchronized
    fun deny(server: MinecraftServer, selector: MatterSelectorKey) {
        loadIfNeeded(server)
        values = values.toMutableMap().also { it[selector] = ExplicitMatterValue.Deny(ExplicitMatterSource.RUNTIME) }
        save(server)
    }

    @Synchronized
    fun reset(server: MinecraftServer, selector: MatterSelectorKey) {
        loadIfNeeded(server)
        values = values.toMutableMap().also { it.remove(selector) }
        save(server)
    }

    @Synchronized
    fun clear(server: MinecraftServer) {
        loadIfNeeded(server)
        values = emptyMap()
        save(server)
    }

    private fun loadIfNeeded(server: MinecraftServer) {
        val path = filePath(server)
        if (loadedFrom == path) {
            return
        }
        loadedFrom = path
        values = MatterSelectorStorageCodec.load(path, ExplicitMatterSource.RUNTIME)
    }

    private fun save(server: MinecraftServer) {
        val path = filePath(server)
        MatterSelectorStorageCodec.save(path, values)
    }

    private fun filePath(server: MinecraftServer): Path =
        server.getWorldPath(LevelResource.ROOT).resolve("data/replicated_integration/matter_overrides.json")
}
