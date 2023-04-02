package com.example.chatapp_chatify.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.example.chatapp_chatify.Repository.FirebaseMessageRepository
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.DataClass.StatusImages
import com.example.chatapp_chatify.DataClass.UserStatus
import com.example.chatapp_chatify.Repository.FirebaseUserRepository
import com.example.chatapp_chatify.utils.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirebaseMessagesViewModel @Inject constructor(private val firebaseMessageRepository: FirebaseMessageRepository, val firebaseUserRepository: FirebaseUserRepository, val userManager: UserManager) : ViewModel() {


    fun sendMessage(
        senderRoom: String,
        receiverRoom: String,
        randomKey: String,
        lastMessageData: HashMap<String, Any>,
        data: MessagesModel,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            firebaseMessageRepository.sendMessage(senderRoom,receiverRoom,randomKey, lastMessageData, data,context)
        }
    }

    val startListeningToMessages : LiveData<ArrayList<MessagesModel>>
        get() = firebaseMessageRepository._messageList

    fun startListeningToMessages(senderRoom: String){
            firebaseMessageRepository.startListeningToMessages(senderRoom)

    }

//    private var _fetchedMessages = MutableLiveData<List<MessagesModel>>()
//    val fetchedMessages : LiveData<List<MessagesModel>>
//        get() = _fetchedMessages

//    fun getMessages(senderRoom: String){
//            _fetchedMessages.value = firebaseMessageRepository.getMessages(senderRoom).value
//    }

    private var _fetchedLocalCacheMessages = MutableLiveData<List<MessagesModel>>()
    val fetchedLocalCacheMessages : LiveData<List<MessagesModel>>
        get() = _fetchedLocalCacheMessages
    fun fetchMessagesForOffLine(senderRoom: String)
    {
            _fetchedLocalCacheMessages.value = firebaseMessageRepository.fetchMessages(senderRoom).asLiveData().value
    }

    fun addPreviousMessagesToLocalCache(){
        viewModelScope.launch(Dispatchers.IO)
        {
            firebaseMessageRepository.addPreviousMessagesToDao()
        }
    }

    // ADDING STATUS IMAGES INTO FIREBASE STORAGE

    private var _storyImageUrl = MutableLiveData<String>()

    val storyImageUrl : LiveData<String>
        get() = _storyImageUrl
    fun addStoryImageToFirebaseStorage(uri: Uri, fileName: String, path : String)
    {
        viewModelScope.launch {
            _storyImageUrl = firebaseUserRepository.uploadPhotoToFirebaseStorage(uri,fileName,path) as MutableLiveData<String>

        }
    }

    private var _storyUploadStatus = MutableLiveData<String>()
    val storyUploadStatus  : LiveData<String>
        get() = _storyUploadStatus
    fun sendUserStory(data : HashMap<String,Any>,status: StatusImages){
       _storyUploadStatus = firebaseMessageRepository.sendUserStory(data,status) as MutableLiveData<String>
    }

    // LISTENING TO UPLOADED STORIES

    val startListeningToStories: LiveData<ArrayList<UserStatus>>
        get() = firebaseMessageRepository._storyList
    fun startListeningToStories(){
       firebaseMessageRepository.startListeningToStories()

    }

    fun uploadUserStory(userStatusData: UserStatus) {
      //  _storyUploadStatus = firebaseMessageRepository.sendUserStory(userStatusData, status) as MutableLiveData<String>

    }



    // ADDING RECORDING INTO FIREBASE STORAGE

    private var _recodingMessageUrl = MutableLiveData<String>()
    val recodingMessageUrl : LiveData<String>
        get() = _recodingMessageUrl
    fun addRecordingMessageToFirebaseStorage(uri: Uri, fileName: String, path : String)
    {
        viewModelScope.launch {
            _recodingMessageUrl = firebaseUserRepository.uploadPhotoToFirebaseStorage(uri,fileName,path) as MutableLiveData<String>

        }
    }


}