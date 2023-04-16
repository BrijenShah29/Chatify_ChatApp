package com.example.chatapp_chatify.CallingActivities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
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
import com.example.chatapp_chatify.databinding.ActivityVideoConferenceBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.UserManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class VideoConferenceActivity : AppCompatActivity() {

    lateinit var binding : ActivityVideoConferenceBinding
    private var isAudioPermissionGranted = false
    private var isCameraPermissionGranted = false
    private var isTokenChannelReceived = false
    private var isCallIncoming = false
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    val REQUEST_CODE = 111

    private val oppositeUserName = ""
    private var caller :String? = null

    //private val tempToken = "007eJxTYDj4ZiOnEM93A/NLx17avIh6VDWn/l9p57Wr59c8ODeT73ufAoO5aZKlYXJqiqWZmamJhZFpkllqIhCnGBiaWBqaGZkcaTZJaQhkZPhYH8HEyACBIL4oQ3JGYoljQUE8iM5Mq4wPy0xJzWdgAAACnCnF"
    private val appId = "75b91ced96654825b6eab6ed01491624"
    private val appCertificate = "049e5679c0ac4693b8d1ba291801c179"
    private var channelName =" "
    private val uid = 0

    private var currentReceiverUser : Users? = null
    private var callType : Int? = null

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
        binding = ActivityVideoConferenceBinding.inflate(layoutInflater)

        isAudioPermissionGranted = ActivityCompat.checkSelfPermission(applicationContext,permissions[0]) == PackageManager.PERMISSION_GRANTED
        isCameraPermissionGranted = ActivityCompat.checkSelfPermission(applicationContext,permissions[1]) == PackageManager.PERMISSION_GRANTED

        if(!isAudioPermissionGranted || !isCameraPermissionGranted){
            ActivityCompat.requestPermissions(this,permissions,REQUEST_CODE)
        }


        // GET BUNDLE FROM PREVIOUS ACTIVITY

        val bundle: Bundle? = intent.extras
        callType = Constant.CALL_TYPE_VIDEO
        if(bundle?.getParcelable<Users>("User")!=null)
        {
            currentReceiverUser =  bundle.getParcelable<Users>("User")
        }
        val data = intent.extras
        if(!data?.getString("channelToken").isNullOrBlank()) {
            channelName = data!!.getString("channelToken").toString()
            isTokenChannelReceived = true
            isCallIncoming = true
            caller = data.getString("incoming")
        } else {
            channelName = "VideoCallInvitation,"+UUID.randomUUID().toString().replace("-","").substring(0,6)
        }

        // GENERATING TOKEN
        val tokenBuilder = RtcTokenBuilder2()
        val timeStamp = (System.currentTimeMillis()/1000 + 360).toInt()


        // GETTING CALLING USER DATA



        // GENERATING TOKEN IF IT IS A OUT GOING CALL
       // if(token.isNullOrBlank())
      //  {
       // channelName = "Video,"+UUID.randomUUID().toString().replace("-","").substring(0,6)
            token = tokenBuilder.buildTokenWithUid(appId,appCertificate,channelName,uid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,timeStamp,timeStamp)
            Constant.CALL_TOKEN = token!!

        setupVideoSdkEngine()

        if(!isTokenChannelReceived) {
            // SEND TOKEN TO CHOSEN CONTACT
            viewModel.sendTokenToOppositeUser(
                channelName,
                currentReceiverUser?.uid.toString(),
                currentReceiverUser?.token.toString(),
                callType!!,
                userManager.getUserName().toString(),
                applicationContext,
                auth.currentUser?.uid.toString()
            )

            // DELETE TOKEN AFTER 60 SECONDS
            CoroutineScope(Dispatchers.IO).launch {
                delay(30000)
                viewModel.deleteLastSentToken(
                    currentReceiverUser?.uid.toString(),
                    auth.currentUser?.uid.toString()
                )
            }
        }

        // Compare this token with received token, if there are same, that means the opposite user has joined your conference



        binding.hangupButton.visibility = View.GONE

        binding.incomingCallButton.setOnClickListener {

                joinCall()
        }

        binding.hangupButton.setOnClickListener {
            leaveChannel()

        }

        setContentView(binding.root)
    }

    private fun leaveChannel() {

        if(!isJoined){
            Toast.makeText(this, "Please join the call", Toast.LENGTH_SHORT).show()
            binding.incomingCallButton.visibility = VISIBLE
            binding.hangupButton.visibility = GONE
        }
        else
        {
            agoraEngine!!.leaveChannel()
            Snackbar.make(binding.root,"You Left the call", Snackbar.LENGTH_SHORT).show()

            if(remoteSurfaceView!=null)
            {
                remoteSurfaceView!!.visibility = View.GONE
            }
            if(localSurfaceView!=null){
                localSurfaceView!!.visibility = View.GONE
            }
            isJoined = false
            binding.hangupButton.visibility = GONE
            binding.incomingCallButton.visibility = VISIBLE
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun joinCall() {
        if (!isAudioPermissionGranted || !isCameraPermissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }else
        {
                val option = ChannelMediaOptions()
                option.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                option.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                setupLocalVideo()
                localSurfaceView!!.visibility = VISIBLE
                agoraEngine!!.startPreview()
                agoraEngine!!.joinChannel(token, channelName, uid, option)
                binding.hangupButton.visibility = VISIBLE
                val callId = UUID.randomUUID().toString()
                val data = CallModel(
                    callId,
                    Constant.CALL_TYPE_VIDEO,
                    userManager.getOppositeUserProfileImage(),
                    userManager.getOppositeUserName().toString(),
                    Calendar.getInstance().time.time
                )
                viewModel.addCalls(data)

        }
    }

    private fun setupVideoSdkEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = appId
            config.mEventHandler = mIRtcEngineEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: java.lang.Exception){
            Log.d(Constant.TAG,e.toString())
        }
    }

    private val mIRtcEngineEventHandler : IRtcEngineEventHandler = object : IRtcEngineEventHandler()
    {
        override fun onUserJoined(uid: Int, elapsed: Int) {
           runOnUiThread {
               Toast.makeText(this@VideoConferenceActivity, "Remote user Joined", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
            // add call into Database

        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isJoined = true
            Snackbar.make(binding.root,"$oppositeUserName Joined",Snackbar.LENGTH_SHORT).show()
            runOnUiThread {
                binding.incomingCallButton.visibility = View.GONE
                binding.hangupButton.visibility = View.VISIBLE
            }

        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Snackbar.make(binding.root, "$oppositeUserName is Offline", Snackbar.LENGTH_LONG)
                    .show()
                remoteSurfaceView!!.visibility = View.GONE
                localSurfaceView!!.visibility = View.GONE
                isJoined = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

    }

    private fun setupRemoteVideo(uid:Int)
    {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.receiverVideoView.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT,uid))
        binding.receiverVideoView.visibility = VISIBLE
    }

    private fun setupLocalVideo(){
        localSurfaceView = SurfaceView(baseContext)
        binding.callerVideoView.addView(localSurfaceView)
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(localSurfaceView,VideoCanvas.RENDER_MODE_HIDDEN,0)
        )
        binding.callerVideoView.visibility = VISIBLE
    }

    override fun onPause() {
        super.onPause()
        Thread{
            viewModel.deleteLastSentToken(currentReceiverUser?.uid.toString(),auth.currentUser?.uid.toString())
            database.reference.child("Chats").child("Calls").child(auth.currentUser?.uid.toString()).removeValue()
        }.start()
    }
    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        Thread{
            RtcEngine.destroy()
            agoraEngine = null
            database.reference.child("Chats").child("Calls").child(currentReceiverUser?.uid.toString()).child(auth.currentUser?.uid.toString()).removeValue()
            database.reference.child("Chats").child("Calls").child(auth.currentUser?.uid.toString()).removeValue()
        }.start()

    }


}