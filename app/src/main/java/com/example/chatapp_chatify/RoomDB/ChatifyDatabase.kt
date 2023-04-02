package com.example.chatapp_chatify.RoomDB

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.DataClass.Users


@Database(entities = [Users::class,MessagesModel::class], version = 1, exportSchema = false)
abstract class ChatifyDatabase :RoomDatabase() {

    abstract fun chatifyDao() : ChatifyDao
}