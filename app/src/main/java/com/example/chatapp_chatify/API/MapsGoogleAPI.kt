package com.example.chatapp_chatify.API

import com.example.chatapp_chatify.DataClass.MapsModel.PlacesMarkListModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface MapsGoogleAPI {

    @GET
   fun getNearbyPlaces(@Url url:String) : Call<PlacesMarkListModel>
}