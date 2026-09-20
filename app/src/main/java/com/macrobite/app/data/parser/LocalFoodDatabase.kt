package com.macrobite.app.data.parser

import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealItem
import com.macrobite.app.domain.model.PresetFood

data class FoodDefinition(
    val standardName: String,
    val aliases: List<String>,
    val defaultPortion: String,
    val baseCalories: Int,
    val baseProtein: Float,
    val baseCarbs: Float,
    val baseFats: Float,
    val defaultCategory: MealCategory = MealCategory.LUNCH,
    val baseMl: Int? = null
)

object LocalFoodDatabase {

    val commonPresets = listOf(
        PresetFood("300ml Whole Milk", "300ml", 185, 10f, 15f, 10f, MealCategory.SNACKS, "Quick Calorie"),
        PresetFood("2 Boiled Eggs", "2 large", 155, 13f, 1f, 11f, MealCategory.BREAKFAST, "High Protein"),
        PresetFood("2 Bananas", "2 medium", 210, 2f, 54f, 1f, MealCategory.SNACKS, "Carb Dense"),
        PresetFood("2 tbsp Peanut Butter", "2 tbsp (32g)", 190, 8f, 7f, 16f, MealCategory.SNACKS, "Fat & Calorie"),
        PresetFood("Hostel Mess Thali", "1 plate", 650, 20f, 95f, 22f, MealCategory.LUNCH, "Daily Staple"),
        PresetFood("Chicken Curry + 2 Rotis", "1 bowl + 2 rotis", 480, 32f, 45f, 18f, MealCategory.DINNER, "High Protein"),
        PresetFood("Double Egg Bhurji + 2 Pav", "2 eggs + 2 pav", 420, 22f, 36f, 20f, MealCategory.SNACKS, "Canteen Fav"),
        PresetFood("Oats with Milk & Peanut Butter", "1 bowl", 450, 18f, 58f, 16f, MealCategory.BREAKFAST, "Bulking Meal"),
        PresetFood("Sattu Shake (300ml)", "300ml glass", 320, 20f, 42f, 6f, MealCategory.SNACKS, "Desi Protein"),
        PresetFood("Maggi (Single Pack)", "1 pack (70g)", 310, 6f, 46f, 12f, MealCategory.SNACKS, "Late Night"),
        PresetFood("Paneer Bhurji + 2 Rotis", "150g + 2 rotis", 520, 26f, 42f, 28f, MealCategory.DINNER, "Veg Protein"),
        PresetFood("Soya Chunks Curry + Rice", "1 bowl + 1 cup rice", 430, 28f, 62f, 7f, MealCategory.LUNCH, "Budget Protein")
    )

    val foodDefinitions = listOf(
        // Eggs & Poultry
        FoodDefinition("Boiled Egg", listOf("boiled egg", "egg boiled", "anda boiled", "egg", "eggs"), "1 egg", 78, 6f, 1f, 5f, MealCategory.BREAKFAST),
        FoodDefinition("Egg Bhurji", listOf("egg bhurji", "anda bhurji", "scrambled egg", "scrambled eggs"), "1 plate", 210, 14f, 4f, 15f, MealCategory.SNACKS),
        FoodDefinition("Egg Omelette", listOf("omelette", "omelet", "egg omelette", "anda omelette"), "2 eggs", 220, 14f, 2f, 18f, MealCategory.BREAKFAST),
        FoodDefinition("Chicken Curry", listOf("chicken curry", "chicken gravy", "murgh curry"), "1 cup (assumed 250ml)", 240, 24f, 6f, 12f, MealCategory.LUNCH, baseMl = 250),
        FoodDefinition("Chicken Breast", listOf("chicken breast", "grilled chicken", "boiled chicken"), "150g", 240, 46f, 0f, 5f, MealCategory.LUNCH),
        FoodDefinition("Chicken Biryani", listOf("chicken biryani", "biryani chicken", "biryani"), "1 plate", 580, 28f, 72f, 20f, MealCategory.LUNCH),
        FoodDefinition("Egg Curry", listOf("egg curry", "anda curry"), "2 eggs with gravy", 260, 16f, 8f, 18f, MealCategory.DINNER),

        // Grains, Rice & Breads
        FoodDefinition("White Rice", listOf("rice", "white rice", "chawal", "steamed rice", "boiled rice"), "1 cup (assumed 250ml)", 205, 4f, 45f, 1f, MealCategory.LUNCH, baseMl = 250),
        FoodDefinition("Roti / Chapati", listOf("roti", "rotis", "chapati", "chapatis", "phulka", "phulkas"), "1 roti", 90, 3f, 18f, 1f, MealCategory.LUNCH),
        FoodDefinition("Paratha (Plain)", listOf("paratha", "plain paratha", "parathas"), "1 paratha", 220, 4f, 28f, 10f, MealCategory.BREAKFAST),
        FoodDefinition("Aloo Paratha", listOf("aloo paratha", "alu paratha"), "1 paratha", 280, 5f, 38f, 12f, MealCategory.BREAKFAST),
        FoodDefinition("Paneer Paratha", listOf("paneer paratha"), "1 paratha", 340, 12f, 34f, 16f, MealCategory.BREAKFAST),
        FoodDefinition("Bread Slice", listOf("bread", "bread slice", "slices bread", "white bread", "brown bread", "toast"), "1 slice", 75, 3f, 14f, 1f, MealCategory.BREAKFAST),
        FoodDefinition("Pav", listOf("pav", "pao"), "1 pav", 110, 3f, 22f, 1f, MealCategory.SNACKS),

        // Dairy & Protein Supplements
        FoodDefinition("Whole Milk", listOf("milk", "whole milk", "full cream milk", "doodh"), "1 cup (assumed 250ml)", 155, 8f, 12f, 9f, MealCategory.SNACKS, baseMl = 250),
        FoodDefinition("Curd / Dahi", listOf("curd", "dahi", "yogurt"), "1 cup (assumed 250ml)", 150, 8f, 11f, 8f, MealCategory.LUNCH, baseMl = 250),
        FoodDefinition("Paneer", listOf("paneer", "cottage cheese"), "100g", 265, 18f, 4f, 20f, MealCategory.DINNER),
        FoodDefinition("Paneer Butter Masala", listOf("paneer butter masala", "paneer curry", "shahi paneer", "matar paneer"), "1 cup (assumed 250ml)", 350, 14f, 16f, 26f, MealCategory.DINNER, baseMl = 250),
        FoodDefinition("Whey Protein", listOf("whey", "protein powder", "whey protein", "protein shake"), "1 scoop", 125, 24f, 2f, 2f, MealCategory.SNACKS),
        FoodDefinition("Peanut Butter", listOf("peanut butter", "pb"), "1 tbsp (16g)", 95, 4f, 3f, 8f, MealCategory.SNACKS),
        FoodDefinition("Sattu Shake", listOf("sattu", "sattu drink", "sattu shake"), "1 glass (300ml)", 250, 15f, 35f, 4f, MealCategory.SNACKS, baseMl = 300),

        // Pulses & Veggie Staples
        FoodDefinition("Dal", listOf("dal", "yellow dal", "dal tadka", "daal", "toor dal", "moong dal", "tadka dal", "katori dal"), "1 cup (assumed 250ml)", 180, 9f, 27f, 4f, MealCategory.LUNCH, baseMl = 250),
        FoodDefinition("Dal Makhani", listOf("dal makhani", "makhani dal", "black dal"), "1 cup (assumed 250ml)", 280, 10f, 30f, 14f, MealCategory.DINNER, baseMl = 250),
        FoodDefinition("Chole / Chana", listOf("chole", "chana", "chickpeas", "chana masala"), "1 cup (assumed 250ml)", 260, 12f, 38f, 7f, MealCategory.LUNCH, baseMl = 250),
        FoodDefinition("Rajma", listOf("rajma", "kidney beans", "rajma masala"), "1 cup (assumed 250ml)", 240, 12f, 35f, 6f, MealCategory.LUNCH, baseMl = 250),
        FoodDefinition("Soya Chunks", listOf("soya chunks", "soya curry", "soybean"), "50g raw/1 bowl cooked", 180, 26f, 16f, 1f, MealCategory.LUNCH),
        FoodDefinition("Mixed Veg", listOf("mixed veg", "sabzi", "bhaji", "aloo gobi", "veg curry"), "1 cup (assumed 250ml)", 140, 3f, 18f, 6f, MealCategory.LUNCH, baseMl = 250),

        // South Indian & Breakfast
        FoodDefinition("Dosa (Plain)", listOf("plain dosa", "dosa", "dosas"), "1 dosa", 160, 4f, 28f, 4f, MealCategory.BREAKFAST),
        FoodDefinition("Masala Dosa", listOf("masala dosa"), "1 dosa", 260, 5f, 42f, 8f, MealCategory.BREAKFAST),
        FoodDefinition("Idli", listOf("idli", "idlis", "idly"), "1 idli", 65, 2f, 13f, 0f, MealCategory.BREAKFAST),
        FoodDefinition("Sambar", listOf("sambar", "sambhar"), "1 cup (assumed 250ml)", 120, 5f, 20f, 2f, MealCategory.BREAKFAST, baseMl = 250),
        FoodDefinition("Poha", listOf("poha", "pohe"), "1 plate (150g)", 230, 4f, 42f, 6f, MealCategory.BREAKFAST),
        FoodDefinition("Upma", listOf("upma"), "1 plate", 220, 5f, 38f, 6f, MealCategory.BREAKFAST),
        FoodDefinition("Oats", listOf("oats", "oatmeal"), "1 bowl (50g oats)", 190, 6f, 33f, 3f, MealCategory.BREAKFAST),

        // Fruits, Snacks & Nuts
        FoodDefinition("Banana", listOf("banana", "bananas", "kela"), "1 medium", 105, 1f, 27f, 0f, MealCategory.SNACKS),
        FoodDefinition("Apple", listOf("apple", "apples", "seb"), "1 medium", 95, 0f, 25f, 0f, MealCategory.SNACKS),
        FoodDefinition("Roasted Peanuts", listOf("peanuts", "roasted peanuts", "groundnuts", "moongfali"), "50g", 290, 13f, 8f, 24f, MealCategory.SNACKS),
        FoodDefinition("Almonds", listOf("almonds", "badam"), "10 pieces (15g)", 90, 3f, 3f, 8f, MealCategory.SNACKS),
        FoodDefinition("Maggi Noodles", listOf("maggi", "instant noodles", "noodles"), "1 pack", 310, 6f, 46f, 12f, MealCategory.SNACKS),
        FoodDefinition("Samosa", listOf("samosa", "samosas"), "1 piece", 220, 3f, 24f, 13f, MealCategory.SNACKS),
        FoodDefinition("Tea / Chai", listOf("chai", "tea", "milk tea"), "1 cup (assumed 150ml)", 75, 2f, 10f, 3f, MealCategory.SNACKS, baseMl = 150),
        FoodDefinition("Coffee", listOf("coffee", "milk coffee"), "1 cup (assumed 150ml)", 85, 2f, 11f, 3f, MealCategory.SNACKS, baseMl = 150)
    )
}
