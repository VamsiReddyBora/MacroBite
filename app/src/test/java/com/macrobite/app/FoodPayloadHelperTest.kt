package com.macrobite.app

import com.macrobite.app.data.parser.FoodPayloadHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodPayloadHelperTest {

    @Test
    fun testWeightAppendedToNameOverridesOldPortion() {
        // User uploaded 5 rupee Haldiram matar which was initially estimated as 13gm.
        // User then edited name by appending "20g pack".
        val result = FoodPayloadHelper.resolve(
            rawName = "Haldirams matar snack packet 20g pack",
            rawPortion = "13gm"
        )

        assertEquals("20g pack", result.portion)
        assertEquals("Haldirams matar snack packet 20g pack", result.fullQuery)
    }

    @Test
    fun testWeightInGramsInName() {
        val result = FoodPayloadHelper.resolve(
            rawName = "GNC pro weight gainer powder 70 grams with water",
            rawPortion = "1 serving"
        )

        assertEquals("70 grams", result.portion)
        assertEquals("GNC pro weight gainer powder 70 grams with water", result.fullQuery)
    }

    @Test
    fun testSeparatePortionUsedWhenNameHasNoWeight() {
        val result = FoodPayloadHelper.resolve(
            rawName = "Dosa with sambar",
            rawPortion = "2 pieces"
        )

        assertEquals("2 pieces", result.portion)
        assertEquals("2 pieces Dosa with sambar", result.fullQuery)
    }

    @Test
    fun testDefaultOneServingFallback() {
        val result = FoodPayloadHelper.resolve(
            rawName = "Black coffee",
            rawPortion = ""
        )

        assertEquals("1 serving", result.portion)
        assertEquals("Black coffee", result.fullQuery)
    }
}
