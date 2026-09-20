package com.macrobite.app.domain.model

data class DailyApiUsage(
    val date: String = "",
    val totalTokens: Int = 0,
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val requestCount: Int = 0
) {
    val estimatedFreeTierLimit: Int = 1500

    val estimatedRemainingRequests: Int
        get() = (estimatedFreeTierLimit - requestCount).coerceAtLeast(0)

    val usagePercentage: Float
        get() = if (estimatedFreeTierLimit > 0) {
            (requestCount.toFloat() / estimatedFreeTierLimit).coerceIn(0f, 1f)
        } else 0f

    val averageTokensPerRequest: Int
        get() = if (requestCount > 0) totalTokens / requestCount else 0
}
