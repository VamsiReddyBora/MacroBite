package com.macrobite.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.macrobite.app.R
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.formatMacroOneDecimal
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.CarbsGreenSubtle
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.FatsCoralSubtle
import com.macrobite.app.ui.theme.ProteinBlue
import com.macrobite.app.ui.theme.ProteinBlueSubtle

@Composable
fun MealCategoryHeader(
    category: MealCategory,
    meals: List<MealEntry>,
    modifier: Modifier = Modifier
) {
    val totalCal = meals.sumOf { it.calories }
    val totalProt = meals.fold(0f) { acc, m -> acc + m.protein }
    val totalCarbs = meals.fold(0f) { acc, m -> acc + m.carbs }
    val totalFats = meals.fold(0f) { acc, m -> acc + m.fats }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val emoji = when (category) {
                MealCategory.BREAKFAST -> "🍳"
                MealCategory.LUNCH -> "🍛"
                MealCategory.SNACKS -> "☕"
                MealCategory.DINNER -> "🍲"
            }
            Text(
                text = "$emoji  ${category.displayName}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (meals.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔥 ${totalCal} kcal  •  🥩 ${totalProt.formatMacroOneDecimal()}g  🌾 ${totalCarbs.formatMacroOneDecimal()}g  🥑 ${totalFats.formatMacroOneDecimal()}g",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MealItemCard(
    meal: MealEntry,
    onDelete: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Meal photo thumbnail or stylized icon badge - fixed uniform 46.dp size
            if (!meal.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = meal.photoUri,
                    contentDescription = meal.foodName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_restaurant),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Food details & macro pills (single line layout to keep cards strictly equal size)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = meal.foodName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Modern Macro Mini-Pills with emojis
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MacroMiniPill(
                        text = "🔥 ${meal.calories} kcal",
                        color = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    MacroMiniPill(
                        text = "🥩 ${meal.protein.formatMacroOneDecimal()}g",
                        color = ProteinBlue,
                        bgColor = ProteinBlueSubtle
                    )
                    MacroMiniPill(
                        text = "🌾 ${meal.carbs.formatMacroOneDecimal()}g",
                        color = CarbsGreen,
                        bgColor = CarbsGreenSubtle
                    )
                    MacroMiniPill(
                        text = "🥑 ${meal.fats.formatMacroOneDecimal()}g",
                        color = FatsCoral,
                        bgColor = FatsCoralSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete action
            IconButton(
                onClick = { onDelete(meal) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete meal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroMiniPill(
    text: String,
    color: Color,
    bgColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = color
        )
    }
}
