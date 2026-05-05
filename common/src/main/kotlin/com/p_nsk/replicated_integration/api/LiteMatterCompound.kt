package com.p_nsk.replicated_integration.api
@JvmRecord
data class LiteMatterCompound(
    val values: Map<LiteResourceLocation, Double>,
) {
    init {
        require(values.values.all { it > 0.0 }) {
            "Matter values must be positive"
        }
    }

    fun add(other: LiteMatterCompound): LiteMatterCompound {
        if (other.values.isEmpty()) return this
        if (values.isEmpty()) return other

        val result = values.toMutableMap()
        for ((matter, amount) in other.values) {
            result[matter] = (result[matter] ?: 0.0) + amount
        }
        return LiteMatterCompound(result.filterValues { it > 0.0 })
    }
    @Suppress("unused")
    fun subtract(other: LiteMatterCompound): LiteMatterCompound {
        if (other.values.isEmpty()) return this

        val result = values.toMutableMap()
        for ((matter, amount) in other.values) {
            val next = (result[matter] ?: 0.0) - amount
            if (next <= 0.0) {
                result.remove(matter)
            } else {
                result[matter] = next
            }
        }
        return LiteMatterCompound(result)
    }

    fun multiply(amount: Double): LiteMatterCompound {
        require(amount >= 0.0) { "amount must not be negative" }
        if (amount == 0.0) return EMPTY
        return LiteMatterCompound(values.mapValues { (_, value) -> value * amount })
    }

    fun divide(amount: Double): LiteMatterCompound {
        require(amount > 0.0) { "amount must be positive" }
        return multiply(1.0 / amount)
    }

    fun weight(): Double =
        values.values.sum()

    fun isBetterThan(other: LiteMatterCompound): Boolean =
        weight() < other.weight()

    companion object {
        @JvmField
        val EMPTY = LiteMatterCompound(emptyMap())

        @JvmStatic
        fun single(matter: LiteResourceLocation, amount: Double): LiteMatterCompound =
            LiteMatterCompound(mapOf(matter to amount))
    }
}
