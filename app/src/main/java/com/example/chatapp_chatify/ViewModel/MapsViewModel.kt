package com.example.chatapp_chatify.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp_chatify.DataClass.MapsModel.PlacesMarkListModel
import com.example.chatapp_chatify.Repository.MapsGoogleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor( private val mapsGoogleRepository: MapsGoogleRepository) : ViewModel()
{
    val data : LiveData<PlacesMarkListModel>
        get() = mapsGoogleRepository.data
    fun getMapsLocation(url: String) {
        viewModelScope.launch {
            mapsGoogleRepository.getMapsLocation(url)
        }
    }

}