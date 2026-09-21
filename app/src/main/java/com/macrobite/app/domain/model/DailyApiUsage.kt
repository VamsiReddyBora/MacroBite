package com.macrobite.app.domain.model

data class DailyApiUsage(
    val date: String = "",
    val totalTokens: Int = 0,
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val requestCount: Int = 0,
    val externalRequestCount: Int = 0,
    val dailyLimit: Int = 500
) {
    val totalRequestCount: Int
        get() = requestCount + externalRequestCount

    val estimatedRemainingRequests: Int
        get() = (dailyLimit - totalRequestCount).coerceAtLeast(0)

    val usagePercentage: Float
        get() = if (dailyLimit > 0) {
            (totalRequestCount.toFloat() / dailyLimit).coerceIn(0f, 1f)
        } else 0f

    val averageTokensPerRequest: Int
        get() = if (requestCount > 0) totalTokens / requestCount else 0

    val estimatedFreeTierLimit: Int
        get() = dailyLimit
}
