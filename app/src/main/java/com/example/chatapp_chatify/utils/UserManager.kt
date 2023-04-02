package com.example.chatapp_chatify.utils

import android.content.Context
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_GUEST
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_ID
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_ID_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_IMAGE
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_IMAGE_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_NAME
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_NAME_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_NUMBER
import com.example.chatapp_chatify.utils.Constant.Companion.OPPOSITE_USER_NUMBER_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.USER_GUEST
import com.example.chatapp_chatify.utils.Constant.Companion.USER_ID
import com.example.chatapp_chatify.utils.Constant.Companion.USER_ID_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.USER_IMAGE
import com.example.chatapp_chatify.utils.Constant.Companion.USER_IMAGE_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.USER_NAME
import com.example.chatapp_chatify.utils.Constant.Companion.USER_NAME_FILE
import com.example.chatapp_chatify.utils.Constant.Companion.USER_NUMBER
import com.example.chatapp_chatify.utils.Constant.Companion.USER_NUMBER_FILE
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UserManager @Inject constructor(@ApplicationContext context: Context){


    private var prefsName = context.getSharedPreferences(USER_NAME_FILE,Context.MODE_PRIVATE)
    private var prefsNumber = context.getSharedPreferences(USER_NUMBER_FILE,Context.MODE_PRIVATE)
    private var prefsUserImage = context.getSharedPreferences(USER_IMAGE_FILE,Context.MODE_PRIVATE)
    private var prefsUserId = context.getSharedPreferences(USER_ID_FILE,Context.MODE_PRIVATE)

    private var prefsOppositeUserName = context.getSharedPreferences(OPPOSITE_USER_NAME_FILE,Context.MODE_PRIVATE)
    private var prefsOppositeNumber = context.getSharedPreferences(OPPOSITE_USER_NUMBER_FILE,Context.MODE_PRIVATE)
    private var prefsOppositeUserImage = context.getSharedPreferences(OPPOSITE_USER_IMAGE_FILE,Context.MODE_PRIVATE)
    private var prefsOppositeUserId = context.getSharedPreferences(OPPOSITE_USER_ID_FILE,Context.MODE_PRIVATE)


    fun saveUserName(username : String?){
        val editor = prefsName.edit()
        editor.putString(USER_NAME,username)
        editor.apply()
    }

    fun saveUserImage(customerImage : String?){
        val editor = prefsUserImage.edit()
        editor.putString(USER_IMAGE,customerImage)
        editor.apply()
    }

    fun saveUserId(customerId : String?){
        val editor = prefsUserId.edit()
        editor.putString(USER_ID,customerId)
        editor.apply()
    }

    fun savePhoneNumber(phoneNumber : String?){
        val editor = prefsNumber.edit()
        editor.putString(USER_NUMBER,phoneNumber)
        editor.apply()
    }

    fun getUserName() : String? {
        return prefsName.getString(USER_NAME, USER_GUEST)
    }

    fun getUserNumber() : String? {
        return prefsNumber.getString(USER_NUMBER,null)
    }


    fun getUserProfileImage() : String? {
        return prefsUserImage.getString(USER_IMAGE,Constant.USER_IMAGE_FILE)
    }

    fun getUserId() : String? {
        return prefsUserId.getString(USER_ID,null)
    }



    // OPPOSITE USER DETAILS

    fun saveOppositeUserName(username : String?){
        val editor = prefsOppositeUserName.edit()
        editor.putString(OPPOSITE_USER_NAME,username)
        editor.apply()
    }

    fun saveOppositeUserImage(customerImage : String?){
        val editor = prefsOppositeUserImage.edit()
        editor.putString(OPPOSITE_USER_IMAGE,customerImage)
        editor.apply()
    }

    fun saveOppositeUserId(customerId : String?){
        val editor = prefsOppositeUserId.edit()
        editor.putString(OPPOSITE_USER_ID,customerId)
        editor.apply()
    }

    fun saveOppositePhoneNumber(phoneNumber : String?){
        val editor = prefsOppositeNumber.edit()
        editor.putString(OPPOSITE_USER_NUMBER,phoneNumber)
        editor.apply()
    }

    fun getOppositeUserName() : String? {
        return prefsOppositeUserName.getString(OPPOSITE_USER_NAME, OPPOSITE_USER_GUEST)
    }

    fun getOppositeUserNumber() : String? {
        return prefsOppositeNumber.getString(OPPOSITE_USER_NUMBER,null)
    }


    fun getOppositeUserProfileImage() : String? {
        return prefsOppositeUserImage.getString(OPPOSITE_USER_IMAGE,Constant.OPPOSITE_USER_IMAGE_FILE)
    }

    fun getOppositeUserId() : String? {
        return prefsOppositeUserId.getString(OPPOSITE_USER_ID,null)
    }





}