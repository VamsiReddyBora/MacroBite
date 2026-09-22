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
        assertEquals(0, usage.externalRequestCount)
        assertEquals(500, usage.dailyLimit)
        assertEquals(0, usage.totalRequestCount)
        assertEquals(500, usage.estimatedRemainingRequests)
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
            requestCount = 10,
            externalRequestCount = 0,
            dailyLimit = 500
        )
        assertEquals(490, usage.estimatedRemainingRequests)
        assertEquals(1500, usage.averageTokensPerRequest)
        assertEquals(10f / 500f, usage.usagePercentage, 0.0001f)
    }

    @Test
    fun testCrossAppExternalRequestTracking() {
        // App used 1 request
        val usage = DailyApiUsage(
            date = "2026-09-16",
            totalTokens = 1200,
            requestCount = 1,
            externalRequestCount = 0,
            dailyLimit = 500
        )
        assertEquals(1, usage.totalRequestCount)
        assertEquals(499, usage.estimatedRemainingRequests)
        assertEquals(1f / 500f, usage.usagePercentage, 0.0001f)
    }

    @Test
    fun testStandardFlashTwentyRpdCap() {
        val usage = DailyApiUsage(
            date = "2026-09-16",
            requestCount = 15,
            externalRequestCount = 0,
            dailyLimit = 20
        )
        assertEquals(15, usage.totalRequestCount)
        assertEquals(5, usage.estimatedRemainingRequests)
        assertEquals(15f / 20f, usage.usagePercentage, 0.0001f)
    }

    @Test
    fun testQuotaRemainingDoesNotGoBelowZero() {
        val usage = DailyApiUsage(
            date = "2026-09-16",
            totalTokens = 2000000,
            promptTokens = 1500000,
            candidateTokens = 500000,
            requestCount = 600,
            dailyLimit = 500
        )
        assertEquals(0, usage.estimatedRemainingRequests)
        assertEquals(1.0f, usage.usagePercentage, 0.0001f)
        assertTrue(usage.averageTokensPerRequest > 0)
    }
}
