package com.macrobite.app

import com.macrobite.app.data.parser.LocalFoodParser
import com.macrobite.app.domain.model.MealCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalFoodParserTest {

    private lateinit var parser: LocalFoodParser

    @Before
    fun setUp() {
        parser = LocalFoodParser()
    }

    @Test
    fun testParseMessLunch() {
        val input = "Mess lunch: 2 cups rice, chicken curry, dal"
        val result = parser.parse(input)

        assertEquals(MealCategory.LUNCH, result.category)
        assertTrue("Expected at least 3 items, got ${result.items.size}", result.items.size >= 3)

        val rice = result.items.find { it.name.contains("Rice", ignoreCase = true) }
        assertNotNull("White rice should be recognized", rice)
        assertTrue("2 cups rice should be around 410 kcal", rice!!.calories >= 350)

        val chicken = result.items.find { it.name.contains("Chicken", ignoreCase = true) }
        assertNotNull("Chicken curry should be recognized", chicken)
        assertTrue("Chicken curry should have significant protein", chicken!!.protein >= 20f)

        val dal = result.items.find { it.name.contains("Dal", ignoreCase = true) }
        assertNotNull("Dal should be recognized", dal)
        assertTrue("Dal should have protein and carbs", dal!!.protein > 0f && dal.carbs > 0f)
    }

    @Test
    fun testParseBreakfastEggsAndBread() {
        val input = "Breakfast: 3 Boiled Eggs + 2 Bread Slices"
        val result = parser.parse(input)

        assertEquals(MealCategory.BREAKFAST, result.category)
        assertEquals(2, result.items.size)

        val eggs = result.items.find { it.name.contains("Egg", ignoreCase = true) }
        assertNotNull(eggs)
        assertEquals(234, eggs!!.calories) // 3 * 78
        assertEquals(18f, eggs.protein, 0.01f) // 3 * 6

        val bread = result.items.find { it.name.contains("Bread", ignoreCase = true) }
        assertNotNull(bread)
        assertEquals(150, bread!!.calories) // 2 * 75
    }

    @Test
    fun testParseHostelBulkingShake() {
        val input = "Snacks: 300ml whole milk, 2 tbsp peanut butter, 1 banana"
        val result = parser.parse(input)

        assertEquals(MealCategory.SNACKS, result.category)
        assertEquals(3, result.items.size)

        val totalCalories = result.items.sumOf { it.calories }
        assertTrue("Total shake calories should be around 450-550 kcal, got $totalCalories", totalCalories in 450..600)
    }

    @Test
    fun testParsePresetItem() {
        val input = "Hostel Mess Thali"
        val result = parser.parse(input)

        assertTrue(result.items.isNotEmpty())
        val thali = result.items.first()
        assertEquals("Hostel Mess Thali", thali.name)
        assertEquals(650, thali.calories)
        assertEquals(20f, thali.protein, 0.01f)
    }

    @Test
    fun testParseOneCupDalAssumes250ml() {
        val input = "1 cup of dal"
        val result = parser.parse(input)

        val dal = result.items.find { it.name.contains("Dal", ignoreCase = true) }
        assertNotNull("Dal should be recognized", dal)
        assertEquals("1 cup (assumed 250ml)", dal!!.portion)
        assertEquals(180, dal.calories)
        assertEquals(9f, dal.protein, 0.01f)
        assertEquals(27f, dal.carbs, 0.01f)
        assertEquals(4f, dal.fats, 0.01f)
    }

    @Test
    fun testParseExplicitMlDalProportionalCalculation() {
        val input = "150ml dal"
        val result = parser.parse(input)

        val dal = result.items.find { it.name.contains("Dal", ignoreCase = true) }
        assertNotNull("Dal should be recognized", dal)
        assertEquals("150ml", dal!!.portion)
        // 150ml / 250ml = 0.6x -> 180 * 0.6 = 108 kcal
        assertEquals(108, dal.calories)
        assertEquals(5.4f, dal.protein, 0.1f)
        assertEquals(16.2f, dal.carbs, 0.1f)
        assertEquals(2.4f, dal.fats, 0.1f)
    }

    @Test
    fun testParseTwoCupsDalAssumes500ml() {
        val input = "2 cups dal"
        val result = parser.parse(input)

        val dal = result.items.find { it.name.contains("Dal", ignoreCase = true) }
        assertNotNull("Dal should be recognized", dal)
        assertEquals("2 cups (assumed 500ml)", dal!!.portion)
        assertEquals(360, dal.calories)
    }

    @Test
    fun testParseHalfCupDalAssumes125ml() {
        val input = "half cup dal"
        val result = parser.parse(input)

        val dal = result.items.find { it.name.contains("Dal", ignoreCase = true) }
        assertNotNull("Dal should be recognized", dal)
        assertEquals("0.5 cup (assumed 125ml)", dal!!.portion)
        assertEquals(90, dal.calories)
    }

    @Test
    fun testParsePlainDalAssumes250ml() {
        val input = "dal"
        val result = parser.parse(input)

        val dal = result.items.find { it.name.contains("Dal", ignoreCase = true) }
        assertNotNull("Dal should be recognized", dal)
        assertEquals("1 cup (assumed 250ml)", dal!!.portion)
        assertEquals(180, dal.calories)
    }
}
