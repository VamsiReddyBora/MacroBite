package com.macrobite.app

import com.macrobite.app.data.local.entity.CustomFoodEntity
import com.macrobite.app.domain.model.CustomFood
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomFoodTest {

    @Test
    fun testCustomFoodDomainToEntityAndBack() {
        val domain = CustomFood(
            id = 42L,
            name = "1 Boiled Egg",
            portion = "50g",
            calories = 75,
            protein = 6.3f,
            carbs = 0.6f,
            fats = 5.2f
        )

        val entity = CustomFoodEntity.fromDomain(domain)
        assertEquals(domain.id, entity.id)
        assertEquals(domain.name, entity.name)
        assertEquals(domain.portion, entity.portion)
        assertEquals(domain.calories, entity.calories)
        assertEquals(domain.protein, entity.protein, 0.01f)
        assertEquals(domain.carbs, entity.carbs, 0.01f)
        assertEquals(domain.fats, entity.fats, 0.01f)

        val restored = entity.toDomain()
        assertEquals(domain.id, restored.id)
        assertEquals(domain.name, restored.name)
        assertEquals(domain.portion, restored.portion)
        assertEquals(domain.calories, restored.calories)
        assertEquals(domain.protein, restored.protein, 0.01f)
        assertEquals(domain.carbs, restored.carbs, 0.01f)
        assertEquals(domain.fats, restored.fats, 0.01f)
    }

    @Test
    fun testCustomFoodMacroCalculation() {
        val whey = CustomFood(
            id = 1L,
            name = "Whey Protein",
            portion = "1 scoop (30g)",
            calories = 120,
            protein = 24.5f,
            carbs = 3.2f,
            fats = 1.8f
        )
        assertEquals(120, whey.calories)
        assertEquals(24.5f, whey.protein, 0.01f)
        assertEquals(3.2f, whey.carbs, 0.01f)
        assertEquals(1.8f, whey.fats, 0.01f)
    }
}
