package com.macrobite.app.domain.repository

import com.macrobite.app.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesForDate(date: String): Flow<List<ChatMessage>>
    fun getDistinctChatDates(): Flow<List<String>>
    suspend fun saveMessage(date: String, message: ChatMessage)
    suspend fun updateLoggedStatus(messageId: String, isLogged: Boolean)
    suspend fun deleteChatForDate(date: String)
}
