package com.example.chatapp_chatify.FirebaseServices

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.chatapp_chatify.CallingActivities.AudioConferenceActivity
import com.example.chatapp_chatify.CallingActivities.VideoConferenceActivity
import com.example.chatapp_chatify.MainActivity
import com.example.chatapp_chatify.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseService : FirebaseMessagingService(){
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.notification?.body.toString().split(",")
        sendNotification(message.notification?.title.toString(),message.notification?.body.toString(),type)

    }

        private fun sendNotification(title: String, messageBody: String, type: List<String>) {
            var intent = Intent()

            var notificationMessage :String = " "

            if(type.contains("AudioCallInvitation"))
            {
                notificationMessage = "Audio Call Invitation"
                intent = Intent(this,AudioConferenceActivity::class.java)
                intent.putExtra("incoming","incoming Call")
                intent.putExtra("channelToken",messageBody)
                Log.d("notificationToken",messageBody.toString())
            }
            else if(type.contains("VideoCallInvitation"))
            {
                notificationMessage = "Video Call Invitation"
                intent = Intent(this, VideoConferenceActivity::class.java)
                intent.putExtra("incoming","incoming Call")
                intent.putExtra("channelToken", messageBody)
            }
//            else if(type.contains("\uD83D\uDCCD Location"))
//            {
//                notificationMessage = "\uD83D\uDCCD Location"
//                intent = Intent(this, MainActivity::class.java)
//            }
            else
            {
                notificationMessage = messageBody
                intent = Intent(this, MainActivity::class.java)
            }

             intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE)

            val channelId = "333"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.chat)
                .setContentTitle(title)
                .setContentText(notificationMessage)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId,
                    "333",
                    NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
        }
    }