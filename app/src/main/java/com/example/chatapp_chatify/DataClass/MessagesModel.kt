package com.example.chatapp_chatify.DataClass

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "MessagesTable", indices = [Index(value = ["messageId"], unique = true)])
data class MessagesModel(
        @PrimaryKey
        @NonNull
        @ColumnInfo(name = "messageId")
        val messageId:String = "",
        var message : String? ="",
        val senderId : String? ="",
        val timestamp : Long ? = 0,
        var messageReaction : Int ? = 6,
        val senderRoom : String?="",
        var messageType : Int ? = 0
    )

{

}