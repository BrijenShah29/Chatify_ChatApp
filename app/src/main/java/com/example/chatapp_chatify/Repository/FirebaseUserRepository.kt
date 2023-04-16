package com.example.chatapp_chatify.Repository

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.chatapp_chatify.DataClass.StatusImages
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.Repository.RepoInterface.FirebaseListenerRepository
import com.example.chatapp_chatify.RoomDB.ChatifyDao
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.Constant.Companion.TAG
import com.example.chatapp_chatify.utils.Constant.Companion.stories
import com.google.firebase.auth.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject

class FirebaseUserRepository @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage,
    val chatifyDao: ChatifyDao,

    ) {


    // CHECKING CURRENT USER LOGGED IN OR NOT
    fun authNullCheck(): Boolean {
        return auth.currentUser != null
    }


    // UPLOAD IMAGE TO FIREBASE

    var photoUploadStatus: FirebaseListenerRepository? = null
    var imageResponse = MutableLiveData<String>()
    var imageLink: String? = null
    fun uploadPhotoToFirebaseStorage(uri: Uri, fileName: String, path: String) {

        val myRef = firebaseStorage.reference.child("$path/$fileName")
        myRef.putFile(uri)
            .addOnSuccessListener {
                myRef.downloadUrl
                    .addOnSuccessListener {
                        photoUploadStatus?.isUploadToFirebaseSuccessful(
                            true,
                            Constant.TAG,
                            it,
                            fileName
                        )
                        imageResponse.value = it.toString()
                    }
                    .addOnFailureListener {

                        photoUploadStatus?.isUploadToFirebaseSuccessful(
                            false,
                            it.message.toString(),
                            null,
                            null
                        )
                    }
                    .addOnFailureListener {
                        photoUploadStatus?.isUploadToFirebaseSuccessful(false, "FAILED", null, null)
                    }
            }
    }


    fun deleteImageFromFireStorage(fileName: String) {
        val myRef = firebaseStorage.getReference("/images/$fileName")
        myRef.delete()
            .addOnSuccessListener {
                Log.d(Constant.TAG, "deleteImageFromFireStorage: ${Constant.SUCCESS_MESSAGE}")
            }
            .addOnFailureListener {
                Log.d(Constant.TAG, "deleteImageFromFireStorage: ${it.message}")
            }
    }


// UPLOAD USER PROFILE INTO FIREBASE


    private var response = MutableLiveData<Boolean>()
    fun uploadDataIntoFirebase(users: Users, path: String): LiveData<Boolean> {
        db.reference.child(path).child(users.phoneNumber!!).setValue(users).addOnSuccessListener {
            response?.value = true
        }
            .addOnFailureListener {
                response?.value = false
            }
        return response

    }


    //GETTING USER PROFILE FROM FIREBASE

    private var userProfile = MutableLiveData<Users>()

    fun fetchUserProfileFromFirebase(path: String, phoneNumber: String): LiveData<Users> {
        db.reference.child(path).child(phoneNumber).get().addOnSuccessListener {
            userProfile?.value = it.getValue(Users::class.java)
        }

        return userProfile
    }


    // GETTING USERS INFO FROM FIREBASE

    private var _userData = MutableLiveData<ArrayList<Users>>()
    private val userData: LiveData<ArrayList<Users>>
        get() = _userData

    fun getUserProfilesFromFirestore(path: String): LiveData<ArrayList<Users>> {

        val usersList = ArrayList<Users>()
        db.reference.child(path).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                for (snapshots in snapshot.children) {
                    val users = snapshots.getValue(Users::class.java)
                    usersList.add(users!!)
                    Log.d("fetchedUsers", users?.name.toString())
                }
                _userData.value?.clear()
                _userData.value = usersList
            }


            override fun onCancelled(error: DatabaseError) {
                Log.d("UserProfileRetrieval", "User data retrieval failed")
            }
        })

        return userData
    }


    // UPLOAD AUDIO TO FIREBASE

    var audioUploadStatus: FirebaseListenerRepository? = null
    private var audioResponse = MutableLiveData<String>()

    fun uploadAudioToFirebaseStorage(uri: Uri, fileName: String, path: String): LiveData<String> {


        val myRef = firebaseStorage.reference.child("$path/$fileName")
        myRef.putFile(uri)

            .addOnSuccessListener {
                myRef.downloadUrl
                    .addOnSuccessListener {
                        photoUploadStatus?.isUploadToFirebaseSuccessful(
                            true,
                            Constant.TAG,
                            it,
                            fileName
                        )
                        imageResponse?.value = it.toString()
                    }
                    .addOnFailureListener {

                        photoUploadStatus?.isUploadToFirebaseSuccessful(
                            false,
                            it.message.toString(),
                            null,
                            null
                        )
                    }
                    .addOnFailureListener {
                        photoUploadStatus?.isUploadToFirebaseSuccessful(false, "FAILED", null, null)
                    }

            }
        return imageResponse
    }

    private var statusReport = MutableLiveData<String>()
    fun deleteLastStory() {
        try {
            db.reference.child("stories").child(auth.uid.toString()).child("status")
                .child(stories.last().timeStamp.toString())
                .removeValue()
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
    }

    fun deleteAllStories() {
        db.reference.child("stories").child(auth.uid.toString()).removeValue()
    }

    private var updateStatus = MutableLiveData<String>()
    fun updateUserProfile(user: HashMap<String, Any>): MutableLiveData<String> {
        var status: String? = " "
        db.reference.child("Users").child(auth.currentUser?.phoneNumber.toString())
            .updateChildren(user).addOnSuccessListener {
            status = Constant.SUCCESS_MESSAGE
        }
        updateStatus.value = status!!
        return updateStatus
    }

}