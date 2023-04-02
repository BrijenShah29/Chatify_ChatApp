package com.example.chatapp_chatify.DI

import android.content.Context
import com.example.chatapp_chatify.Repository.FirebaseMessageRepository
import com.example.chatapp_chatify.Repository.FirebaseUserRepository
import com.example.chatapp_chatify.RoomDB.ChatifyDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {

    @Provides
    fun providesFirebaseRepository(auth: FirebaseAuth, db : FirebaseDatabase, storage: FirebaseStorage, chatifyDao : ChatifyDao) : FirebaseUserRepository {
        return FirebaseUserRepository(auth,db,storage,chatifyDao)
    }

    @Provides
    fun providesFirebaseMessageRepository(auth: FirebaseAuth, db : FirebaseDatabase, storage: FirebaseStorage, chatifyDao : ChatifyDao) : FirebaseMessageRepository{
        return FirebaseMessageRepository(auth,db,storage,chatifyDao)
    }




}
