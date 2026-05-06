package com.p_nsk.replicated_integration.api.model
/**
 * ResourceLocation but independent of Minecraft, and with a more specific purpose.
 * */
@JvmRecord
data class LiteResourceLocation(val namespace: String, val path: String) : Comparable<LiteResourceLocation> {
    companion object {
        private val NAMESPACE_PATTERN = Regex("[a-z0-9_.-]+")
        private val PATH_PATTERN = Regex("[a-z0-9_./-]+")

        @JvmStatic
        fun of(namespace: String, path: String): LiteResourceLocation {
            return LiteResourceLocation(namespace, path)
        }
    }

    init {
        if (namespace.isBlank()) {
            throw IllegalArgumentException("Namespace must not be blank")
        }
        if (path.isBlank()) {
            throw IllegalArgumentException("Path must not be blank")
        }
        require(namespace.matches(NAMESPACE_PATTERN)) { "Invalid namespace: $namespace" }
        require(path.matches(PATH_PATTERN)) { "Invalid path: $path" }
    }

    override fun compareTo(other: LiteResourceLocation): Int {
        return compareValuesBy(this, other, LiteResourceLocation::namespace, LiteResourceLocation::path)
    }

    override fun toString(): String {
        return "$namespace:$path"
    }

}
