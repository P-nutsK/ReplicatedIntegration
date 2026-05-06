package com.p_nsk.replicated_integration.data

import com.p_nsk.replicated_integration.api.ExplicitMatterSource
import com.p_nsk.replicated_integration.api.ExplicitMatterValue
import com.p_nsk.replicated_integration.api.MatterSelectorKey
import com.p_nsk.replicated_integration.api.MatterSelectorStorageCodec
import net.minecraft.server.MinecraftServer
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Files
import java.nio.file.Path

object NeoMatterConfigOverrides {
    private var loadedFrom: Path? = null
    private var values: Map<MatterSelectorKey, ExplicitMatterValue> = emptyMap()

    @Synchronized
    fun snapshot(): Map<MatterSelectorKey, ExplicitMatterValue> {
        loadIfNeeded()
        return values
    }

    @Synchronized
    fun commit(server: MinecraftServer): Int {
        loadIfNeeded()
        val runtime = NeoMatterRuntimeOverrides.snapshot(server)
        if (runtime.isEmpty()) {
            return 0
        }
        values = values.toMutableMap().also { it.putAll(runtime.mapValues { (_, value) -> value.asConfigValue() }) }
        save()
        NeoMatterRuntimeOverrides.clear(server)
        return runtime.size
    }

    private fun loadIfNeeded() {
        val path = filePath()
        if (loadedFrom == path) {
            return
        }
        loadedFrom = path
        values = MatterSelectorStorageCodec.load(path, ExplicitMatterSource.CONFIG)
    }

    private fun save() {
        val path = filePath()
        MatterSelectorStorageCodec.save(path, values)
    }

    private fun filePath(): Path =
        FMLPaths.CONFIGDIR.get().resolve("replicated_integration-matter_overrides.json")

    private fun ExplicitMatterValue.asConfigValue(): ExplicitMatterValue =
        when (this) {
            is ExplicitMatterValue.Deny -> ExplicitMatterValue.Deny(ExplicitMatterSource.CONFIG)
            is ExplicitMatterValue.Set -> ExplicitMatterValue.Set(compound, ExplicitMatterSource.CONFIG)
        }
}
