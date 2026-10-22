package com.kisss.visualtime.data

data class RecentDurationsInitialization(
    val durations: List<Int>,
    val encodedValueToSeed: String?,
)

object RecentDurationsPolicy {
    const val MAX_ITEMS = 3
    val Defaults = listOf(15, 25, 40)

    fun initialize(
        preferenceExists: Boolean,
        encodedValue: String?,
    ): RecentDurationsInitialization {
        if (!preferenceExists) {
            return RecentDurationsInitialization(
                durations = Defaults,
                encodedValueToSeed = encode(Defaults),
            )
        }
        return RecentDurationsInitialization(
            durations = decode(encodedValue.orEmpty()),
            encodedValueToSeed = null,
        )
    }

    fun recordStarted(current: List<Int>, durationMinutes: Int): List<Int> =
        (listOf(durationMinutes) + current)
            .filter(::isValid)
            .distinct()
            .take(MAX_ITEMS)

    fun encode(durations: List<Int>): String =
        durations.filter(::isValid).distinct().take(MAX_ITEMS).joinToString(",")

    fun decode(encodedValue: String): List<Int> =
        encodedValue
            .split(',')
            .mapNotNull { token -> token.toIntOrNull()?.takeIf(::isValid) }
            .distinct()
            .take(MAX_ITEMS)

    private fun isValid(durationMinutes: Int): Boolean = durationMinutes in 1..1_440
}
