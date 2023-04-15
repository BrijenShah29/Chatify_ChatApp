package com.example.chatapp_chatify.DataClass

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.chatapp_chatify.utils.Constant
import java.sql.Timestamp

@Entity(tableName = "callTable", indices = [Index(value = ["callId"], unique = true)])
data class CallModel(
    @PrimaryKey
    val callId : String ="",
    val callFormat : Int?=Constant.CALL_TYPE_AUDIO,
    val callerImage : String?="",
    val callerName : String? ="",
    val timestamp: Long? = 0,
    val callType : String? =Constant.INCOMING_CALL
)