package com.example.chatapp_chatify.CallingActivities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.chatapp_chatify.AgoraTokenGenerator.RtcTokenBuilder2
import com.example.chatapp_chatify.DataClass.CallModel
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.MainActivity
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.databinding.ActivityAudioConferenceBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject



@AndroidEntryPoint
class AudioConferenceActivity : AppCompatActivity() {

    lateinit var binding : ActivityAudioConferenceBinding
    private var isAudioPermissionGranted = false
    private var isCameraPermissionGranted = false
    private var isTokenChannelReceived = false
    private var isCallIncoming = false
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    val REQUEST_CODE = 111

    private val oppositeUserName = ""

    private val appId = "75b91ced96654825b6eab6ed01491624"
    private val appCertificate = "049e5679c0ac4693b8d1ba291801c179"
    private var channelName :String?= null
    private val uid = 0

    private var currentReceiverUser : Users? = null
    private var callType : Int? = null
    private var caller :String? = null

    private var token :String?= null

    @Inject
    lateinit var auth : FirebaseAuth
    @Inject
    lateinit var database : FirebaseDatabase

    private var isJoined = false
    private var agoraEngine : RtcEngine? = null
    private var localSurfaceView : SurfaceView? = null
    private var remoteSurfaceView : SurfaceView?=null

    private val viewModel by viewModels<FirebaseMessagesViewModel>()

    @Inject
    lateinit var userManager : UserManager

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE){
            isAudioPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            isCameraPermissionGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioConferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GET PARCELABLE FROM MAIN ACTIVITY

        isAudioPermissionGranted = ActivityCompat.checkSelfPermission(this,permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!isAudioPermissionGranted){
            ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE)
        }

        // GET BUNDLE FROM PREVIOUS ACTIVITY

        val bundle: Bundle? = intent.extras
        callType = Constant.CALL_TYPE_AUDIO
        if(bundle?.getParcelable<Users>("User")!=null)
        {
            currentReceiverUser =  bundle.getParcelable<Users>("User")
        }
        val data = intent.extras
        if(!data?.getString("channelToken").isNullOrBlank()) {
            channelName = null
            channelName = data!!.getString("channelToken").toString()
            isTokenChannelReceived = true
            isCallIncoming = true
            binding.callerNameTextview.text = getString(R.string.incoming)
            caller = data.getString("incoming")
            Log.d("notificationToken",channelName.toString())
        } else {
            channelName = null
            channelName = "AudioCallInvitation,"+UUID.randomUUID().toString().replace("-","").substring(0,6)
        }



        // GENERATING TOKEN
        val tokenBuilder = RtcTokenBuilder2()
        val timeStamp = (System.currentTimeMillis()/1000 + 60).toInt()

        // GENERATING TOKEN IF IT IS A OUT GOING CALL

            token = tokenBuilder.buildTokenWithUid(appId,appCertificate,channelName,uid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,timeStamp,timeStamp)
            Constant.CALL_TOKEN = token!!

            // SEND TOKEN TO CHOSEN CONTACT
        if(!isTokenChannelReceived)
        {
            viewModel.sendTokenToOppositeUser(channelName,currentReceiverUser?.uid.toString(),currentReceiverUser?.token.toString(),
                callType!!,userManager.getUserName().toString(),this,auth.currentUser?.uid.toString())

            // DELETE TOKEN AFTER 30 SECONDS
            CoroutineScope(Dispatchers.IO).launch {
                delay(30000)
                viewModel.deleteLastSentToken(currentReceiverUser?.uid.toString(),auth.currentUser?.uid.toString())
            }
        }
        if(isCallIncoming)
        {
            binding.callerNameTextview.text = caller
        }


        // Compare this token with received token, if there are same, that means the opposite user has joined your conference


        setupAudioSdkEngine()
        binding.callerNameTextview.text = currentReceiverUser?.name.toString()

        binding.hangupButton.visibility = View.GONE




        binding.incomingCallButton.setOnClickListener {
            if(!isAudioPermissionGranted){
                ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE)

            }else
            {
                joinChannel()
            }
        }
        binding.hangupButton.setOnClickListener {
            agoraEngine?.leaveChannel()
            val intent = Intent(this@AudioConferenceActivity,MainActivity::class.java)
            startActivity(intent)
            finish()

        }


    }
    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinChannel()
    {
        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        agoraEngine!!.joinChannel(token,channelName,uid,option)
        binding.hangupButton.visibility = View.VISIBLE
        // add call into Database
        val callId = UUID.randomUUID().toString()
        val data = CallModel(
            callId,
            Constant.CALL_TYPE_AUDIO,
            userManager.getOppositeUserProfileImage().toString(),
            userManager.getOppositeUserName().toString(),
            Calendar.getInstance().time.time
        )
        viewModel.addCalls(data)
    }


    private fun setupAudioSdkEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            binding.timerChronometer.start()
            runOnUiThread { binding.callerNameTextview.text = "Remote user joined: $uid" }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            isJoined = true
            showMessage("Joined Channel $channel")
            runOnUiThread { binding.callerNameTextview.text = "you joined the call" }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            runOnUiThread {
            showMessage("Remote user offline $uid $reason")
            binding.timerChronometer.stop()
            if(isJoined)
            {
                binding.callerNameTextview.text = "Waiting for $oppositeUserName to join"
            }
                onBackPressedDispatcher.onBackPressed()
            }

        }

        override fun onLeaveChannel(stats: RtcStats) {
            // Listen for the local user leaving the channel
            runOnUiThread {
                binding.timerChronometer.stop()
                binding.hangupButton.visibility = View.INVISIBLE
                binding.callerNameTextview.text = "Press the button to join call"
                isJoined = false
            }

        }
    }

    override fun onPause() {
        super.onPause()
        Thread {
            viewModel.deleteLastSentToken(currentReceiverUser?.uid.toString(),
                auth.currentUser?.uid.toString())
        }.start()

    }

    override fun onDestroy() {
        super.onDestroy()
        Thread {
            database.reference.child("Chats").child("Calls").child(currentReceiverUser?.uid.toString()).child(auth.currentUser?.uid.toString()).removeValue()
        }.start()
    }
}