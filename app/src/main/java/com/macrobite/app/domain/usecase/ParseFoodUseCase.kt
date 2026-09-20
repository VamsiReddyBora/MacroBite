package com.macrobite.app.domain.usecase

import android.graphics.Bitmap
import com.macrobite.app.data.parser.GeminiFoodParser
import com.macrobite.app.data.parser.ParseResult
import com.macrobite.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParseFoodUseCase @Inject constructor(
    private val geminiFoodParser: GeminiFoodParser,
    private val preferencesRepository: UserPreferencesRepository
) {

    suspend fun parseText(input: String): ParseResult {
        val apiKey = preferencesRepository.getGeminiApiKey().first()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is required for online logging. Please check Settings.")
        }
        return geminiFoodParser.parseText(input, apiKey)
    }

    suspend fun parseImage(bitmap: Bitmap, promptNote: String?): ParseResult {
        val apiKey = preferencesRepository.getGeminiApiKey().first()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is required for photo analysis. Please check Settings.")
        }
        return geminiFoodParser.parseImage(bitmap, promptNote, apiKey)
    }
}
