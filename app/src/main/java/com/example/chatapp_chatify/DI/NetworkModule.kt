package com.example.chatapp_chatify.DI

import com.example.chatapp_chatify.API.MapsGoogleAPI
import com.example.chatapp_chatify.utils.Constant.Companion.BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    @Singleton
    @Provides
    fun providesRetrofit() : Retrofit {
        return Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(BASE_URL).build()
    }

    @Singleton
    @Provides
     fun providesMapsGoogleAPI(retrofit: Retrofit) : MapsGoogleAPI{
         return retrofit.create(MapsGoogleAPI::class.java)
     }
}