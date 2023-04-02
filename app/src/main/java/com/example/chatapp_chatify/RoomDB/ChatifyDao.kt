package com.example.chatapp_chatify.RoomDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chatapp_chatify.DataClass.MessagesModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatifyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdatedMessages(messagesModel: List<MessagesModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleUpdatedMessage(messagesModel: MessagesModel)

    @Query("SELECT * FROM MessagesTable WHERE senderRoom = :senderRoom")
    fun getMessages(senderRoom: String): Flow<List<MessagesModel>>



    @Query("DELETE FROM MessagesTable WHERE messageId = :id ")
    suspend fun deleteSingleMessage(id:String)

    @Query("DELETE FROM MessagesTable where senderRoom = :senderRoom")
    suspend fun deleteMessagesFromSelectedConversation(senderRoom:String)

}