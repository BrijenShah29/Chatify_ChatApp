package com.example.chatapp_chatify.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.example.chatapp_chatify.DataClass.CallModel
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
        context: Context,
        token: String?,
        userName: String?
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            firebaseMessageRepository.sendMessage(senderRoom,receiverRoom,randomKey, lastMessageData, data,context,token,userName)
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

   // private var _storyImageUrl = MutableLiveData<String>()
    val storyImageUrl : LiveData<String>
        get() = firebaseUserRepository.imageResponse
    fun addStoryImageToFirebaseStorage(uri: Uri, fileName: String, path : String)
    {
        viewModelScope.launch {
            firebaseUserRepository.uploadPhotoToFirebaseStorage(uri,fileName,path)
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

   // private var _imageMessageUrl = MutableLiveData<String>()
    val imageMessageUrl : LiveData<String>
        get() = firebaseUserRepository.imageResponse
    fun addImageMessageToFirebaseStorage(uri: Uri, fileName: String, path : String)
    {
        viewModelScope.launch {
           firebaseUserRepository.uploadPhotoToFirebaseStorage(uri,fileName,path)

        }
    }


    // LISTENING TO USER'S ONLINE OR OFFLINE STATUS

    private var _startListeningToUserPresence = MutableLiveData<String>()
     val startListeningToUserPresence : LiveData<String>
         get() = firebaseMessageRepository._userPrescenceListner

    fun startListeningToUserPresence(receiverUid: String){
        firebaseMessageRepository.startListeningToUserPresence(receiverUid)

    }

    // GETTING TOKEN OF CURRENT USER AND STORING INTO FIREBASE
    fun getUserTokenAndUpdateUserData() {
        firebaseMessageRepository.getUserTokenAndUpdateUserData()
    }

    fun deleteLastStory() {
        firebaseUserRepository.deleteLastStory()
    }

    fun deleteAllStories() {
        firebaseUserRepository.deleteAllStories()
    }

    fun sendTokenToOppositeUser(
        token: String?,
        receiverId: String,
        notificationToken: String,
        callType: Int,
        userName: String?,
        context: Context,
        senderId: String
    ) {
        firebaseMessageRepository.sendCallingTokenToOppositeUser(token,receiverId,notificationToken,callType,userName!!,context,senderId)
    }

    fun deleteLastSentToken(userId: String, senderId: String?) {

        firebaseMessageRepository.deleteLastSentToken(userId,senderId!!)

    }

    // LISTENING TO USER CALLING TOKENS
    val userCallingToken : LiveData<String>
        get() = firebaseMessageRepository._userCallingTokenListner

    fun startListeningToCallingTokens(userId: String)
    {
        firebaseMessageRepository.receiverCallingTokens(userId)
    }

    // FETCHING CALL LOGS

    private var _fetchedCallLog = MutableLiveData<List<CallModel>>()
    val fetchedCallLog  : LiveData<List<CallModel>>
        get() = _fetchedCallLog
    fun getCallLog()
    {
        _fetchedCallLog = firebaseMessageRepository.getCallLog().asLiveData() as MutableLiveData<List<CallModel>>
    }

    // Adding calls into call log
    fun addCalls(call:CallModel)
    {
        viewModelScope.launch {
            firebaseMessageRepository.insertCalls(call)
        }
    }



}