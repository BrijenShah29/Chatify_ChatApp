package com.example.chatapp_chatify.Repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.chatapp_chatify.API.MapsGoogleAPI
import com.example.chatapp_chatify.DataClass.MapsModel.PlacesMarkListModel
import com.example.chatapp_chatify.utils.Constant.Companion.TAG
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.net.URL
import javax.inject.Inject

class MapsGoogleRepository @Inject constructor(
    val retrofit: Retrofit,
    val mapsGoogleAPI: MapsGoogleAPI,
) {

    val data = MutableLiveData<PlacesMarkListModel>()
    fun getMapsLocation(url: String) {
        mapsGoogleAPI.getNearbyPlaces(url).enqueue(object : Callback<PlacesMarkListModel> {
            override fun onResponse(
                call: Call<PlacesMarkListModel>,
                response: Response<PlacesMarkListModel>,
            ) {
                if (response.isSuccessful) {
                    data.value = response.body()

                } else {
                    Log.d(TAG, "Response is not successful")
                }


            }

            override fun onFailure(call: Call<PlacesMarkListModel>, t: Throwable) {
                Log.d(TAG, "Response is not successful")

            }

        })
    }

}