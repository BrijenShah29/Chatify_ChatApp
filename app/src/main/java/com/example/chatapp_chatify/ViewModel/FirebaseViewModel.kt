package com.example.chatapp_chatify.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.Repository.FirebaseUserRepository
import com.example.chatapp_chatify.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirebaseViewModel @Inject constructor(private val firebaseUserRepository: FirebaseUserRepository, val userManager: UserManager) : ViewModel()
{


    // CHECK IF USER SIGNED IN OR NOT
    var loginStatus : MutableLiveData<Boolean>? = null

    init {
        viewModelScope.async {
            loginStatus?.value = firebaseUserRepository.authNullCheck()
        }
    }

    // UPLOADING IMAGE TO FIREBASE STORAGE

    private var _profileImageUploadedUrl = MutableLiveData<String>()

    val profileImageUploadedUrl : LiveData<String>
        get() = firebaseUserRepository.imageResponse

    fun uploadProfileImageToFirebaseStorage(uri: Uri,fileName: String,path : String){

        viewModelScope.launch {
           firebaseUserRepository.uploadPhotoToFirebaseStorage(uri,fileName,path)

        }



    }

    // DELETE IMAGE FROM FIREBASE STORAGE
    fun deleteImageFromFireStorage(path: String,fileName: String) {
        viewModelScope.launch {
        firebaseUserRepository.deleteImageFromFireStorage(fileName)
        }
    }


    // STORE USER PROFILE INTO DB

    private var _uploadStatus = MutableLiveData<Boolean>()
   val uploadStatus : LiveData<Boolean>
       get() = _uploadStatus

    fun uploadDataIntoFirebase(users: Users, path: String){
        viewModelScope.launch {
            _uploadStatus = firebaseUserRepository.uploadDataIntoFirebase(users,path) as MutableLiveData<Boolean>


        }
    }


    // FETCHING SINGLE USER PROFILE FROM FIREBASE
    private var _fetchedUserProfile = MutableLiveData<Users>()

    val fetchedUserProfile : LiveData<Users>
        get() = _fetchedUserProfile

    fun fetchUserProfileFromFirebase(path: String,phoneNumber : String){
        viewModelScope.launch {
            _fetchedUserProfile = firebaseUserRepository.fetchUserProfileFromFirebase(path,
                phoneNumber) as MutableLiveData<Users>
        }
        }



// FETCHING ALL USER PROFILES FROM FIREBASE

    private var _fetchedAllUsersList = MutableLiveData<ArrayList<Users>>()
    val fetchedAllUsersList : LiveData<ArrayList<Users>>
        get() = _fetchedAllUsersList

     fun getUserProfilesFromFirestore(path: String) {
         viewModelScope.async{
             _fetchedAllUsersList = firebaseUserRepository.getUserProfilesFromFirestore(path) as MutableLiveData<ArrayList<Users>>
         }
     }


    private var _updatedUserProfile = MutableLiveData<String>()
    val updatedUserProfile : LiveData<String>
        get() = _updatedUserProfile

    fun updateUserProfile(user: HashMap<String, Any>){
        _updatedUserProfile = firebaseUserRepository.updateUserProfile(user)
    }


}