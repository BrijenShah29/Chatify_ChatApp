package com.example.chatapp_chatify.Repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.chatapp_chatify.DataClass.CallModel
import com.example.chatapp_chatify.RoomDB.ChatifyDao
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.DataClass.StatusImages
import com.example.chatapp_chatify.DataClass.UserStatus
import com.example.chatapp_chatify.utils.Constant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class FirebaseMessageRepository @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage,
    val chatifyDao: ChatifyDao,
    val userFirebaseMessaging: FirebaseMessaging,
) {

    val database = db.reference.child("Chats")
    fun sendMessage(
        senderRoom: String,
        receiverRoom: String,
        randomKey: String,
        lastMessageData: HashMap<String, Any>,
        data: MessagesModel,
        context: Context,
        token: String?,
        userName: String?,
    ) {

        database.child(senderRoom.toString()).updateChildren(lastMessageData)
        database.child(receiverRoom.toString()).updateChildren(lastMessageData)


        database.child(senderRoom.toString())
            .child("messages")
            .child(randomKey)
            .setValue(data)
            .addOnSuccessListener {

                // ADDING MESSAGE AS LAST MESSAGE

                // ADDING SAME INTO RECEIVER'S DATABASE

                database.child(receiverRoom.toString())
                    .child("messages")
                    .child(randomKey)
                    .setValue(data)
                    .addOnSuccessListener {
                        //  Log.d(Constant.TAG, "Message process successful")
                        //  Log.d( "Success Firebase", "Successfully added new messages in previous pending messages along with date")
                        // SEND NOTIFICATION
                        when (data.messageType) {
                            Constant.MESSAGE_TYPE_TEXT -> {

                                sendNotificationsWithVolley(userName!!,
                                    data.message!!,
                                    token!!,
                                    context)
                            }
                            Constant.MESSAGE_TYPE_IMAGE -> {
                                sendNotificationsWithVolley(userName!!, "Image", token!!, context)
                            }
                            Constant.MESSAGE_TYPE_AUDIO -> {
                                sendNotificationsWithVolley(userName!!, "Audio", token!!, context)
                            }
                            Constant.MESSAGE_TYPE_LOCATION -> {
                                sendNotificationsWithVolley(userName!!,
                                    "\uD83D\uDCCD Location",
                                    token!!,
                                    context)
                            }
                            else -> {
                                Log.d(Constant.TAG, "Wrong Message Type Found")
                            }
                        }

                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            "something went wrong!!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    "Something went Wrong !!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun sendNotificationsWithVolley(
        name: String,
        message: String,
        token: String,
        context: Context,
    ) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://fcm.googleapis.com/fcm/send"
        val data = JSONObject()
        data.put("title", name)
        data.put("body", message)

        val notificationData = JSONObject()
        notificationData.put("notification", data)
        notificationData.put("to", token)


        val request: JsonObjectRequest =
            object : JsonObjectRequest(url, notificationData, Response.Listener {
                // Toast.makeText(ChatActivity.this, "success", Toast.LENGTH_SHORT).show();
            },
                Response.ErrorListener { error ->
                    Toast.makeText(context,
                        error.localizedMessage,
                        Toast.LENGTH_SHORT).show()
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val map: HashMap<String, String> = HashMap()
                    val key =
                        "key=AAAAWPeoLoE:APA91bGbmASsFPDVR3T47jKtHyug_GhIfbx15dJWp_y-q2yWFLGUZtmRALpl88VfWZ7KEdYJfM6W-jG6gW_yYTNZ7rTgOdr4juv3xLpnnfRpd4-q0--vrIAQIIqqZd6XoyAQY3rgeguH"
                    map["Content-Type"] = "application/json"
                    map["Authorization"] = key
                    return map
                }
            }
        queue.add(request)


    }


    var _messageList = MutableLiveData<ArrayList<MessagesModel>>()
    fun startListeningToMessages(senderRoom: String) {
        // GET MESSAGES AND STORE IN DAO
        // val startDate = time
        val endDate = Calendar.getInstance().time.time
        val messageList = kotlin.collections.ArrayList<MessagesModel>()
        var updatedMessaged: MessagesModel? = null
        var deletedMessage: MessagesModel? = null
        var i = 0

        database.child(senderRoom)
            .child("messages").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        val message = snapshot.getValue(MessagesModel::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                        _messageList.value = messageList
                        // ADD INTO ROOM AS NEW MESSAGE
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        // UPDATE INTO ROOM DB
                        updatedMessaged = snapshot.getValue(MessagesModel::class.java)
                        i = 0
                        if (messageList.size > i) {
                            while (messageList.size > i) {
                                if (messageList[i].messageId == updatedMessaged!!.messageId) {
//                            _messageList.value!![i].message = updatedMessaged!!.message
//                            _messageList.value!![i].messageReaction = updatedMessaged!!.messageReaction
                                    messageList[i].messageReaction =
                                        updatedMessaged!!.messageReaction
                                    messageList[i].messageType = updatedMessaged!!.messageType
                                    _messageList.value = messageList
                                    break
                                }
                                i += 1
                            }
                            i = 0
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        if (updatedMessaged != null) {
                            chatifyDao.insertSingleUpdatedMessage(updatedMessaged!!)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        deletedMessage = snapshot.getValue(MessagesModel::class.java)
                        // DELETE MESSAGE IN ROOM
                        for (data in _messageList.value!!) {
                            if (data.messageId == deletedMessage!!.messageId) {
                                _messageList.value!!.remove(data)
                            }
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            if (deletedMessage != null) {
                                chatifyDao.deleteSingleMessage(deletedMessage!!.messageId)
                            }
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })


//            (object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    for (data in snapshot.children){
//                        val message = data.getValue(MessagesModel::class.java)
//                        _messageList.value?.add(message!!)
//                        Log.d("received message", message?.message.toString())
//                        // val diffResult = DiffUtil.calculateDiff(MessagesDiffUtil(oldList, newList))
//                    }
//                }
//                override fun onCancelled(error: DatabaseError) {
//                }
//            })
        // ADDING MESSAGES INTO DAO


    }

    suspend fun addPreviousMessagesToDao() {
        if (_messageList?.value?.isNotEmpty() == true) {
            chatifyDao.insertUpdatedMessages(_messageList.value as List<MessagesModel>)
            _messageList.value!!.clear()
        }
    }

    // to fetch messages from database
    fun fetchMessages(senderRoom: String) = chatifyDao.getMessages(senderRoom)

    // TO INSERR CALLS INTO CALL LOG
    suspend fun insertCalls(call: CallModel) = chatifyDao.insertCall(call)

    // TO FETCH CALLS FROM DATABASE
    fun getCallLog() = chatifyDao.getCallLog()

    // TO UPLOAD USER STORIES
    private var statusReport = MutableLiveData<String>()
    fun sendUserStory(data: HashMap<String, Any>, status: StatusImages): LiveData<String> {
        db.reference.child("stories").child(auth.uid.toString()).updateChildren(data)
            .addOnSuccessListener {
                db.reference.child("stories").child(auth.uid.toString()).child("status")
                    .child(status.timeStamp.toString())
                    .setValue(status).addOnSuccessListener {
                        statusReport.value = "success"
                    }.addOnFailureListener {
                        statusReport.value = "failed"
                    }
            }
        return statusReport
    }

    // TO LISTEN UPDATES OF USER STORIES
    var _storyList = MutableLiveData<ArrayList<UserStatus>>()
    fun startListeningToStories() {

        val storyList: kotlin.collections.ArrayList<UserStatus> = ArrayList()
        var statusImages = kotlin.collections.ArrayList<StatusImages>()
        var previousStory: ArrayList<String> = ArrayList()
        db.reference.child("stories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    storyList.clear()
                    statusImages.clear()
                    _storyList.value?.clear()
                    for (snapshots in snapshot.children) {
                        val uid = snapshots.child("uploaderUid").getValue(String::class.java)
                        val name = snapshots.child("name").getValue(String::class.java)
                        val profileImage =
                            snapshots.child("profileImage").getValue(String::class.java)
                        val lastUpdate =
                            snapshots.child("lastUpdated").getValue(Long::class.java)
                        if (snapshots.child("status") != null) {
                            for (statusImage in snapshots.child("status").children) {
                                val data = statusImage.getValue(StatusImages::class.java)!!
                                statusImages.add(data)
                                Log.d("fetchedStatusURlRepo", data.imageUrl.toString())
                                Log.d("fetchedStatusURlRepo", data.toString())
                            }
                        }
                        val storyData = UserStatus(
                            uid,
                            name,
                            profileImage,
                            lastUpdate,
                            statusImages
                        )
                        storyList.add(storyData!!)
                        Log.d("FoundDataPerUser", storyList.size.toString())
                    }
                    _storyList.value = storyList
                }


            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Stories", "Unable to fetch stories")
            }

        })


    }


    var _userPrescenceListner = MutableLiveData<String>()
    fun startListeningToUserPresence(receiverUid: String) {
        db.reference.child("presence").child(receiverUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        //for (data in snapshot.children) {
                        //val data = snapshot.getValue(UserPresence::class.java)!!
                        // data.key
                        _userPrescenceListner.value = snapshot.value.toString()
                        Log.d("presence", _userPrescenceListner.value.toString())
                        // }
                    }
                }

                //  }
                override fun onCancelled(error: DatabaseError) {
                    Log.d("Error in fetching status", "Error in fetching user presence")
                }
            })
    }


    // SETTING UP FIREBASE USER NOTIFICATIONS, ACQUIRING TOKEN AND UPDATING USER DATA
    fun getUserTokenAndUpdateUserData() {
        userFirebaseMessaging.token.addOnSuccessListener { token ->
            val map = HashMap<String, Any>()
            map["token"] = token.toString()
            db.reference.child("Users").child(auth.currentUser?.phoneNumber!!).updateChildren(map)
        }
    }

    fun sendCallingTokenToOppositeUser(
        token: String?,
        receiverId: String,
        notificationToken: String,
        callType: Int,
        userName: String,
        context: Context,
        senderId: String,

        ) {
        val map = HashMap<String, Any>()
        map["token"] = token.toString()
        database.child("Calls")
            .child(receiverId)
            .child(senderId)
            .updateChildren(map)
            .addOnSuccessListener {
                if (callType == Constant.CALL_TYPE_AUDIO) {
                    sendNotificationsWithVolley(userName, token!!, notificationToken, context)
                } else if (callType == Constant.CALL_TYPE_VIDEO) {
                    sendNotificationsWithVolley(userName, token!!, notificationToken, context)
                }
            }
    }


    var _userCallingTokenListner = MutableLiveData<String>()
    fun receiverCallingTokens(userId: String) {
        database.child("Calls").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        if (!data.value.toString().isNullOrEmpty()) {
                            val tokenArray = data.value.toString().split("=", "}")
                            _userCallingTokenListner.value = tokenArray[1]
                        }
                        // _userCallingTokenListner.value = data?.value.toString()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Call Token Error", error.message.toString())
            }

        })


    }

    fun deleteLastSentToken(receiverId: String, senderId: String) {
        database.child("Calls").child(receiverId).child(senderId).removeValue()
    }


}