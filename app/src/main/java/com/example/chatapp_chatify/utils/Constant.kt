package com.example.chatapp_chatify.utils

import android.content.Intent
import com.example.chatapp_chatify.CallingActivities.AudioConferenceActivity
import com.example.chatapp_chatify.CallingActivities.VideoConferenceActivity
import com.example.chatapp_chatify.DataClass.StatusImages
import com.example.chatapp_chatify.MainActivity

interface Constant {
    companion object {

        const val SUCCESS_MESSAGE = "SUCCESS"
        const val USER_NAME_FILE = "USER_NAME_FILE"
        const val USER_NAME = "USER_NAME"
        const val REGISTERED_USER = "New User"

        const val USER_NUMBER_FILE = "USER_NUMBER_FILE"
        const val USER_NUMBER = "USER_NUMBER"

        const val USER_GUEST = "Guest Profile"
        const val PAGE_HEADER = "AGRO_APP"

        const val USER_IMAGE_FILE = "USER_IMAGE_FILE"
        const val USER_IMAGE = "USER_IMAGE"

        const val USER_ID_FILE = "USER_ID_FILE"
        const val USER_ID = "USER_ID"

        const val TAG ="CHATIFY_APP"

        const val OPPOSITE_USER_NAME_FILE = "OPPOSITE_USER_NAME_FILE"
        const val OPPOSITE_USER_NAME = "OPPOSITE_USER_NAME"

        const val OPPOSITE_USER_NUMBER_FILE = "OPPOSITE_USER_NUMBER_FILE"
        const val OPPOSITE_USER_NUMBER = "OPPOSITE_USER_NUMBER"

        const val OPPOSITE_USER_IMAGE_FILE = "OPPOSITE_USER_IMAGE_FILE"
        const val OPPOSITE_USER_IMAGE = "OPPOSITE_USER_IMAGE"
        const val OPPOSITE_USER_ID_FILE = "OPPOSITE_USER_ID_FILE"
        const val OPPOSITE_USER_ID = "OPPOSITE_USER_ID"

        const val OPPOSITE_USER_GUEST = "Current Chat"

        const val MESSAGE_TYPE_TEXT = 0

        const val MESSAGE_TYPE_IMAGE = 1

        const val MESSAGE_TYPE_AUDIO = 2

        const val MESSAGE_TYPE_LOCATION =3
        const val CALL_TYPE_AUDIO = 0
        const val CALL_TYPE_VIDEO = 1
        const val USER_OFFLINE = 0
        const val USER_ONLINE = 1
        const val INCOMING_CALL ="INCOMING"
        const val OUTGOING_CALL ="OUTGOING"



        const val USER_TOKEN_FILE = "USER_TOKEN_FILE"
        const val USER_TOKEN = "USER_TOKEN"

        val stories = ArrayList<StatusImages>()
        var CALL_TOKEN = ""

        const val BASE_URL = "https://maps.googleapis.com/"




    }

    }
