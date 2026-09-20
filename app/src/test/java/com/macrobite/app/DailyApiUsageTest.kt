package com.macrobite.app

import com.macrobite.app.domain.model.DailyApiUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyApiUsageTest {

    @Test
    fun testDefaultValues() {
        val usage = DailyApiUsage(date = "2026-09-16")
        assertEquals("2026-09-16", usage.date)
        assertEquals(0, usage.totalTokens)
        assertEquals(0, usage.promptTokens)
        assertEquals(0, usage.candidateTokens)
        assertEquals(0, usage.requestCount)
        assertEquals(1500, usage.estimatedRemainingRequests)
        assertEquals(0f, usage.usagePercentage, 0.001f)
        assertEquals(0, usage.averageTokensPerRequest)
    }

    @Test
    fun testUsageCalculations() {
        val usage = DailyApiUsage(
            date = "2026-09-16",
            totalTokens = 15000,
            promptTokens = 11000,
            candidateTokens = 4000,
            requestCount = 10
        )
        assertEquals(1490, usage.estimatedRemainingRequests)
        assertEquals(1500, usage.averageTokensPerRequest)
        assertEquals(10f / 1500f, usage.usagePercentage, 0.0001f)
    }

    @Test
    fun testQuotaRemainingDoesNotGoBelowZero() {
        val usage = DailyApiUsage(
            date = "2026-09-16",
            totalTokens = 2000000,
            promptTokens = 1500000,
            candidateTokens = 500000,
            requestCount = 1600
        )
        assertEquals(0, usage.estimatedRemainingRequests)
        assertEquals(1.0f, usage.usagePercentage, 0.0001f)
        assertTrue(usage.averageTokensPerRequest > 0)
    }
}
