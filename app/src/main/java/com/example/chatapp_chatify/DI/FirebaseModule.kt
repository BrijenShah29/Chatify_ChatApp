package com.example.chatapp_chatify.DI

import android.content.Context
import com.example.chatapp_chatify.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FirebaseModule {

    @Singleton
    @Provides
    fun providesAuthentication() : FirebaseAuth {
        return FirebaseAuth.getInstance()
    }


    @Singleton
    @Provides
    fun providesStorage() : FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Singleton
    @Provides
    fun providesFirebaseDatabase() : FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Singleton
    @Provides
    fun providesUserManagerSharedPreferences(@ApplicationContext context : Context) : UserManager {
        return UserManager(context)
    }

    @Singleton
    @Provides
    fun providesFirebaseNotifications() : FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

}