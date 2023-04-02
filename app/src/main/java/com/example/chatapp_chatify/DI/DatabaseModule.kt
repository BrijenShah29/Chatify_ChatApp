package com.example.chatapp_chatify.DI

import android.content.Context
import androidx.room.Room
import com.example.chatapp_chatify.RoomDB.ChatifyDao
import com.example.chatapp_chatify.RoomDB.ChatifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideChatifyDatabase(@ApplicationContext context : Context) : ChatifyDatabase {
        return Room.databaseBuilder(context, ChatifyDatabase::class.java,"chatify_database").build()

    }

    @Singleton
    @Provides
    fun providesChatifyDao(chatifyDatabase: ChatifyDatabase) : ChatifyDao {
        return chatifyDatabase.chatifyDao()
    }
}