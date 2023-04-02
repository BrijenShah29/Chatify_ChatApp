package com.example.chatapp_chatify.Repository.RepoInterface

import android.net.Uri

interface FirebaseListenerRepository {

    fun isUploadToFirebaseSuccessful(isSuccess : Boolean,status: String,uri: Uri?,imageId:String?) : Boolean
    {

        return isSuccess
    }
}