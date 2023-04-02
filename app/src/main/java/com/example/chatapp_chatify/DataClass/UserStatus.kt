package com.example.chatapp_chatify.DataClass

data class UserStatus(
    val uploaderUid : String?="",
    val name : String? ="",
    val profileImage : String? = "",
    val lastUpdated : Long? = 0,
    val status : ArrayList<StatusImages> = ArrayList()
)
