package com.example.chatapp_chatify.Repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class FirebaseMessageRepository @Inject constructor(val auth: FirebaseAuth, val db: FirebaseDatabase, private val firebaseStorage: FirebaseStorage, val chatifyDao: ChatifyDao)
{
    val database = db.reference.child("Chats")
    fun sendMessage( senderRoom: String,
                             receiverRoom: String,
                             randomKey: String,
                             lastMessageData: HashMap<String, Any>,
                             data: MessagesModel,
                     context: Context
    )
    {

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
                        Log.d(Constant.TAG, "Message process successful")
                        Log.d(
                            "Success Firebase",
                            "Successfully added new messages in previous pending messages along with date")

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


    var _messageList = MutableLiveData<ArrayList<MessagesModel>>()
    fun startListeningToMessages(senderRoom: String){
        // GET MESSAGES AND STORE IN DAO
       // val startDate = time
       val endDate = Calendar.getInstance().time.time
        val messageList = kotlin.collections.ArrayList<MessagesModel>()
        var updatedMessaged : MessagesModel? = null
        var deletedMessage:MessagesModel?= null

        database.child(senderRoom)
            .child("messages").addChildEventListener(object :ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        val message = snapshot.getValue(MessagesModel::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                        _messageList.value = messageList
                        Log.d("received message", message?.message.toString())
                        // ADD INTO ROOM AS NEW MESSAGE
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        if(snapshot.exists()) {
                            // UPDATE INTO ROOM DB
                            updatedMessaged = snapshot.getValue(MessagesModel::class.java)
                            var i = 0
                            while (i <= messageList.size) {
                                if (messageList[i].messageId == updatedMessaged!!.messageId) {
//                            _messageList.value!![i].message = updatedMessaged!!.message
//                            _messageList.value!![i].messageReaction = updatedMessaged!!.messageReaction
                                    messageList[i].messageReaction =
                                        updatedMessaged!!.messageReaction
                                    _messageList.value = messageList
                                    break
                                }
                                i += 1
                            }
                        }


                    CoroutineScope(Dispatchers.IO).launch{
                        if(updatedMessaged!=null)
                        {
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

    suspend fun addPreviousMessagesToDao(){
        if(_messageList?.value?.isNotEmpty() == true)
        {
            chatifyDao.insertUpdatedMessages(_messageList.value as List<MessagesModel>)
            _messageList.value!!.clear()
        }
    }

    // to fetch messages from database
    fun fetchMessages(senderRoom: String) = chatifyDao.getMessages(senderRoom)

// TO UPLOAD USER STORIES
    private var statusReport = MutableLiveData<String>()
    fun sendUserStory(data: HashMap<String, Any>, status: StatusImages) : LiveData<String>
    {
        db.reference.child("stories").child(auth.uid.toString()).updateChildren(data).addOnSuccessListener {
            db.reference.child("stories").child(auth.uid.toString()).child("status").push()
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
    fun startListeningToStories(){
        val storyList : kotlin.collections.ArrayList<UserStatus> = ArrayList()
        var statusImages = kotlin.collections.ArrayList<StatusImages>()
        db.reference.child("stories").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    statusImages.clear()
                    _storyList.value?.clear()
                    storyList.clear()
                    for (snapshots in snapshot.children)
                    {

                        val uid = snapshots.child("uploaderUid").getValue(String::class.java)
                        val name = snapshots.child("name").getValue(String::class.java)
                        val profileImage = snapshots.child("profileImage").getValue(String::class.java)
                        val lastUpdate = snapshots.child("lastUpdated").getValue(Long::class.java)
                        for(statusImage in snapshots.child("status").children)
                        {
                            if(statusImage.exists()){
                                //val url = statusImage.children.
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
                        storyList.add(storyData)

                        Log.d("Stories",name!!)
                        Log.d("Stories",lastUpdate.toString())

                    }
                    _storyList.value = storyList
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Stories","Unable to fetch stories")
            }

        })

    }

}