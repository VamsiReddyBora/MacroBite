package com.macrobite.app.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.macrobite.app.domain.model.MealEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportCsvUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun generateCsvFile(meals: List<MealEntry>): Intent = withContext(Dispatchers.IO) {
        val fileName = "macrobite_export_${System.currentTimeMillis()}.csv"
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("Date,Category,Food Item,Portion,Calories (kcal),Protein (g),Carbs (g),Fats (g),Fiber (g),Sugar (g),Sodium (mg),Vitamins & Minerals\n")
            for (meal in meals) {
                val escapedFood = meal.foodName.replace("\"", "\"\"")
                val escapedPortion = meal.portion.replace("\"", "\"\"")
                val escapedVitamins = meal.vitaminsAndMinerals.replace("\"", "\"\"")
                writer.append("\"${meal.date}\",\"${meal.category.displayName}\",\"$escapedFood\",\"$escapedPortion\",${meal.calories},${meal.protein},${meal.carbs},${meal.fats},${meal.fiber},${meal.sugar},${meal.sodium},\"$escapedVitamins\"\n")
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "MacroBite Daily Log Export")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
