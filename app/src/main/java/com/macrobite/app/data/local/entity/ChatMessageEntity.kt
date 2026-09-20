package com.macrobite.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["chatDate"])]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val chatDate: String, // e.g. "2026-09-16"
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val foodPayloadJson: String?,
    val isLogged: Boolean,
    val imageUri: String? = null,
    val isWebSearch: Boolean = false,
    val actionPayloadJson: String? = null
)
