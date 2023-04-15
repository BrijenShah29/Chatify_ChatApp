package com.example.chatapp_chatify.DataClass

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import javax.annotation.Nonnull

@Entity(tableName = "userTable")
@Parcelize
data class Users
    (
    @PrimaryKey
    @Nonnull
    val uid : String="0",
    val name : String?="",
    val phoneNumber : String?="",
    val profileImage : String?= "",
    val token : String?=""
    ):Parcelable
{

}





