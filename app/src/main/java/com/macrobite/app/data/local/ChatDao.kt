package com.macrobite.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.macrobite.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE chatDate = :date ORDER BY timestamp ASC")
    fun getMessagesForDate(date: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT DISTINCT chatDate FROM chat_messages ORDER BY chatDate DESC")
    fun getDistinctChatDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET isLogged = :isLogged WHERE id = :id")
    suspend fun updateLoggedStatus(id: String, isLogged: Boolean)

    @Query("DELETE FROM chat_messages WHERE chatDate = :date")
    suspend fun deleteMessagesForDate(date: String)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)
}
