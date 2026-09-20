package com.macrobite.app.data.repository

import com.google.gson.Gson
import com.macrobite.app.data.local.ChatDao
import com.macrobite.app.data.local.entity.ChatMessageEntity
import com.macrobite.app.domain.model.JarvisActionPayload
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.ui.chat.ChatFoodPayload
import com.macrobite.app.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val gson: Gson
) : ChatRepository {

    override fun getMessagesForDate(date: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForDate(date).map { list ->
            list.map { entity ->
                val payload = entity.foodPayloadJson?.let { json ->
                    try {
                        gson.fromJson(json, ChatFoodPayload::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                val actionPayload = entity.actionPayloadJson?.let { json ->
                    try {
                        gson.fromJson(json, JarvisActionPayload::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    isUser = entity.isUser,
                    timestamp = entity.timestamp,
                    foodPayload = payload,
                    actionPayload = actionPayload,
                    isLogged = entity.isLogged,
                    imageUri = entity.imageUri,
                    isWebSearch = entity.isWebSearch
                )
            }
        }
    }

    override fun getDistinctChatDates(): Flow<List<String>> {
        return chatDao.getDistinctChatDates()
    }

    override suspend fun saveMessage(date: String, message: ChatMessage) {
        val payloadJson = message.foodPayload?.let { gson.toJson(it) }
        val actionPayloadJson = message.actionPayload?.let { gson.toJson(it) }
        val entity = ChatMessageEntity(
            id = message.id,
            chatDate = date,
            text = message.text,
            isUser = message.isUser,
            timestamp = message.timestamp,
            foodPayloadJson = payloadJson,
            isLogged = message.isLogged,
            imageUri = message.imageUri,
            isWebSearch = message.isWebSearch,
            actionPayloadJson = actionPayloadJson
        )
        chatDao.insertMessage(entity)
    }

    override suspend fun updateLoggedStatus(messageId: String, isLogged: Boolean) {
        chatDao.updateLoggedStatus(messageId, isLogged)
    }

    override suspend fun deleteChatForDate(date: String) {
        chatDao.deleteMessagesForDate(date)
    }
}
