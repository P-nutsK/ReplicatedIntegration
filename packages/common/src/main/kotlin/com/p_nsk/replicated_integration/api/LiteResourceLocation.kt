package com.p_nsk.replicated_integration.api
/**
 * ResourceLocation but independent of Minecraft, and with a more specific purpose.
 * 26で名前が変わるので安全のために
 * */
@JvmRecord
data class LiteResourceLocation(val namespace: String, val path: String) : Comparable<LiteResourceLocation> {

    companion object {
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
    }

    override fun compareTo(other: LiteResourceLocation): Int {
        return compareValuesBy(this, other, LiteResourceLocation::namespace, LiteResourceLocation::path)
    }

    override fun toString(): String {
        return "$namespace:$path"
    }

}
