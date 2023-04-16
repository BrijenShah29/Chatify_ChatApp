package com.example.chatapp_chatify.Fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.devlomi.record_view.OnRecordListener
import com.example.chatapp_chatify.Adapters.MessagesAdapter
import com.example.chatapp_chatify.DataClass.MapsModel.Result
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.MainActivity
import com.example.chatapp_chatify.Fragments.LoginFragments.MapsFragment
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import com.example.chatapp_chatify.databinding.FragmentUserChatBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.Constant.Companion.TAG
import com.example.chatapp_chatify.utils.OnLocationSelectedListener
import com.example.chatapp_chatify.utils.UserManager
import com.github.pgreze.reactions.dsl.reactionConfig
import com.github.pgreze.reactions.dsl.reactions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.isNullOrEmpty
import kotlin.collections.set
import kotlin.collections.toList


@SuppressLint("SuspiciousIndentation", "NotifyDataSetChanged")
@AndroidEntryPoint
class UserChatFragment : Fragment() {


    private var isRecordPermissionGranted = false
    private var isWriteExternalPermissionGranted = false
    private var isImageAboutToSend = false

    private var mapsFragment: MapsFragment? = null


    private var permissions =
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val REQUEST_CODE = 111
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    lateinit var binding: FragmentUserChatBinding
    lateinit var currentUser: Users

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var userManager: UserManager

    lateinit var adapter: MessagesAdapter

    @Inject
    lateinit var database: FirebaseDatabase

    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    private var senderRoom: String? = ""
    private var receiverRoom: String? = ""
    private var oppositeUserToken: String? = ""

    private var imageLink = ""

    lateinit var messageList: List<MessagesModel>

    private var messageLocation: String? = null

    private val viewModel by viewModels<FirebaseViewModel>()
    private val messageViewModel by viewModels<FirebaseMessagesViewModel>()
    private var listening: Boolean = false

    private lateinit var mediaRecorder: MediaRecorder
    private var audioPath: String? = ""


    private var clickedImageUri: Uri? = null
    private var imageUri: Uri? = null

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            binding.imagePreview.setImageURI(null)
            binding.imagePreview.setImageURI(clickedImageUri)
            isImageAboutToSend = true
            binding.sendImageLayout.visibility = VISIBLE
            binding.cancelImage.visibility = VISIBLE
            binding.recordButton.visibility = View.INVISIBLE
            binding.sendButton.visibility = VISIBLE
        } else {
            binding.imagePreview.setImageURI(null)
            binding.sendImageLayout.visibility = GONE
            binding.cancelImage.visibility = GONE
            binding.recordButton.visibility = VISIBLE
            binding.sendButton.visibility = View.INVISIBLE
        }

    }
    private var launchGalleryForActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            clickedImageUri = it.data!!.data
            binding.imagePreview.setImageURI(clickedImageUri)
            isImageAboutToSend = true
            binding.sendImageLayout.visibility = VISIBLE
            binding.cancelImage.visibility = VISIBLE
            binding.recordButton.visibility = View.INVISIBLE
            binding.sendButton.visibility = VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            isRecordPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            isWriteExternalPermissionGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED

        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemAudio: MenuItem = menu.findItem(R.id.audioCall_appBar)
        val itemVideo: MenuItem = menu.findItem(R.id.videoCall_appBar)
        itemAudio.isVisible = true
        itemVideo.isVisible = true


    }

    override fun onStart() {
        super.onStart()

        if (requireArguments().getString("senderRoom") != null) {
            senderRoom = requireArguments().getString("senderRoom")
            messageViewModel.startListeningToMessages(senderRoom!!)


            listening = true

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        if (requireArguments().getParcelable<Users>("UserData") != null) {
            currentUser = requireArguments().getParcelable<Users>("UserData")!!
            oppositeUserToken = currentUser.token
        }
        (requireActivity() as MainActivity).setCurrentReceiverUser(currentUser)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUserChatBinding.inflate(layoutInflater)

        val appbar = binding.appBar

        // (requireActivity() as AppCompatActivity).setSupportActionBar(appbar)

        val actionbar = (requireActivity() as AppCompatActivity).supportActionBar

        actionbar?.setDisplayHomeAsUpEnabled(true)

        binding.attachmentPopup.visibility = GONE
        binding.sendImageLayout.visibility = GONE
        binding.cancelImage.visibility = GONE

        messageList = ArrayList()

        isRecordPermissionGranted = ActivityCompat.checkSelfPermission(requireContext(),
            permissions[0]) == PackageManager.PERMISSION_GRANTED
        isWriteExternalPermissionGranted = ActivityCompat.checkSelfPermission(requireContext(),
            permissions[1]) == PackageManager.PERMISSION_GRANTED
        if (!isRecordPermissionGranted) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_CODE)
        }

        val senderId = auth.currentUser?.uid.toString()
        val receiverId = userManager.getOppositeUserId().toString()
//        val senderNumber = userManager.getUserNumber().toString()
//        val receiverNumber = userManager.getOppositeUserNumber().toString()


        val db = database.reference.child("Chats")
        senderRoom = senderId + receiverId
        receiverRoom = receiverId + senderId

        // START LISTENING TO CHAT
        if (!listening) {
            messageViewModel.startListeningToMessages(senderRoom!!)

        }
//        // START LISTENING TO OPPOSITE USER'S STATUS

        messageViewModel.startListeningToUserPresence(receiverId)

        messageViewModel.startListeningToUserPresence.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer {
                Log.d("MainScreen", it.toString())

                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    if (it != null && it.toInt() != 3) {
                        if (it.toInt() == Constant.USER_ONLINE) {
                            try {
                                (requireActivity() as MainActivity).findViewById<ImageView>(R.id.online_status_image)
                                    .setImageDrawable(ResourcesCompat.getDrawable(
                                        resources,
                                        R.drawable.user_online,
                                        null))
                            } catch (e: Exception) {
                                Log.d(TAG, e.message.toString())
                            }

                            Log.d("MainScreen", it.toString())

                        } else if (it.toInt() == Constant.USER_OFFLINE) {
                            try {
                                (requireActivity() as MainActivity).findViewById<ImageView>(R.id.online_status_image)
                                    .setImageDrawable(ResourcesCompat.getDrawable(
                                        resources,
                                        R.drawable.user_offline,
                                        null))
                            } catch (e: Exception) {
                                Log.d(TAG, e.message.toString())
                            }
                        }
                    } else {
                        try {
                            (requireActivity() as MainActivity).findViewById<ImageView>(R.id.online_status_image)
                                .setImageDrawable(ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.user_offline,
                                    null))
                        } catch (e: Exception) {
                            Log.d(TAG, e.message.toString())
                        }

                    }
                }
            })


        // SETTING UP THE MESSAGE REACTION
        val config = reactionConfig(requireContext()) {
            reactions {
                resId { R.drawable.like }
                resId { R.drawable.laugh }
                resId { R.drawable.love_emoji }
                reaction { R.drawable.wow scale ImageView.ScaleType.FIT_XY }
                reaction { R.drawable.crying scale ImageView.ScaleType.FIT_XY }
                reaction { R.drawable.angry scale ImageView.ScaleType.FIT_XY }
            }
        }

        // SETTING UP CAMERA BUTTON ON CLICK EVENT
        clickedImageUri = createImageUri()
        binding.attachmentCamera.setOnClickListener {
            if (clickedImageUri != null) {
                contract.launch(clickedImageUri)
            } else {
                clickedImageUri = createImageUri()
                contract.launch(clickedImageUri)
            }

        }

        binding.cancelImage.setOnClickListener {
            Log.d("cancel image", "cancel image clicked")
            clickedImageUri = null
            Toast.makeText(requireContext(), "Image Removed.", Toast.LENGTH_SHORT).show()
            binding.imagePreview.setImageURI(null)
            binding.sendImageLayout.visibility = GONE
            binding.cancelImage.visibility = GONE
            binding.recordButton.visibility = VISIBLE
            binding.sendButton.visibility = View.INVISIBLE
        }

        //GETTING DATA FROM LOCAL CACHE FIRST
        messageViewModel.fetchMessagesForOffLine(senderRoom!!)
        messageViewModel.fetchedLocalCacheMessages.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (!it.isNullOrEmpty()) {
                    messageList = it
                }
            })

        //SETTING UP THE RECYCLERVIEW AND ADAPTER
        // messageViewModel.getMessages(senderRoom!!)
        adapter = MessagesAdapter(requireContext(), senderRoom!!, receiverRoom!!, db, config, auth)
        binding.chatRecyclerview.adapter = adapter
        binding.chatRecyclerview.scrollToPosition(messageList.size)

        // GETTING DATA FROM FIREBASE IF THERE'S ANY UPDATE IN MESSAGE
        messageViewModel.startListeningToMessages.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer {
                if (it.isNullOrEmpty()) {
                    binding.progressBar.visibility = GONE
                    binding.chatRecyclerview.smoothScrollToPosition(adapter.itemCount)
                    adapter.submitList(messageList)
                    adapter.notifyDataSetChanged()
                } else {
                    binding.progressBar.visibility = GONE
                    binding.chatRecyclerview.smoothScrollToPosition(it.size - 1)
                    adapter.submitList(it.toList())
                    adapter.notifyDataSetChanged()

                }
            })

        binding.sendButton.setOnClickListener {

            Log.d("sentButtonClicked", "SendButtonClicked")
            if (isImageAboutToSend && binding.sendImageLayout.visibility == VISIBLE) {
                // upload Image to firebase & get the link and fetch the link
                // Uri.parse(userManager.getUserProfileImage())
                binding.progressBar.visibility = VISIBLE
                val fileName =
                    auth.currentUser?.uid.toString() + System.currentTimeMillis() + ".png"
                messageViewModel.addImageMessageToFirebaseStorage(clickedImageUri!!,
                    fileName,
                    "Images/Message")
                messageViewModel.imageMessageUrl.observe(viewLifecycleOwner,
                    androidx.lifecycle.Observer { link ->

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)

                            if (link.isNotEmpty() && link != imageLink) {
                                clickedImageUri = null
                                binding.imagePreview.setImageURI(null)
                                binding.sendImageLayout.visibility = GONE
                                binding.cancelImage.visibility = GONE
                                binding.recordButton.visibility = VISIBLE
                                binding.sendButton.visibility = View.INVISIBLE
                                isImageAboutToSend = false
                                // send the fetched link to message
                                sendImageMessage(senderId, link)
                                imageLink = link
                            } else {
                                Log.d("ImageUrlFromFirebase", "Link is empty")
                            }
                        }
                    })
            } else if (binding.messageInput.text.trim().isNotEmpty()) {
                if (binding.messageInput.text.toString() == messageLocation) {
                    sendTextMessage(senderId, messageLocation!!, Constant.MESSAGE_TYPE_LOCATION)
                } else {
                    sendTextMessage(senderId,
                        binding.messageInput.text.toString(),
                        Constant.MESSAGE_TYPE_TEXT)
                }
            } else {
                Snackbar.make(requireView(), "Please write a message", Snackbar.LENGTH_SHORT).show()
            }
        }


        binding.attachmentButton.setOnClickListener {
            if (binding.attachmentPopup.visibility == GONE || binding.attachmentPopup.visibility == View.INVISIBLE) {
                binding.attachmentPopup.visibility = VISIBLE
            } else {
                binding.attachmentPopup.visibility = GONE
            }
        }

        // SETTING UP TEXT WATCHER

        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().trim().isNotEmpty()) {

                    binding.chatBottomLayout.visibility = VISIBLE
                    binding.recordButton.visibility = GONE
                    binding.sendButton.visibility - VISIBLE
                } else {
                    binding.chatBottomLayout.visibility = VISIBLE
                    binding.recordButton.visibility = VISIBLE
                    binding.sendButton.visibility - GONE
                }
            }

            override fun afterTextChanged(p0: Editable?) {

                if (p0.toString().trim().isNotEmpty()) {

                    binding.chatBottomLayout.visibility = VISIBLE
                    binding.recordButton.visibility = GONE
                    binding.sendButton.visibility - VISIBLE
                } else {
                    binding.chatBottomLayout.visibility = VISIBLE
                    binding.recordButton.visibility = VISIBLE
                    binding.sendButton.visibility - GONE
                }
            }

        })

        // SETTING UP SEND FROM GALLERY BUTTON
        binding.sendImageButton.setOnClickListener {
            // SEND IMAGE
            clickedImageUri = null
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            launchGalleryForActivity.launch(intent)
        }

        // SETTING UP MAPS ACTIVITY FROM MAPS BUTTON

        binding.sendLocationButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putParcelable("UserData", currentUser)
            Navigation.findNavController(requireView())
                .navigate(R.id.action_userChatFragment_to_mapsFragment, bundle)
        }
        // GETTING MAPS DATA
        if (requireArguments().getParcelable<Result>("Location") != null) {
            val result = requireArguments().getParcelable<Result>("Location")
            val placeTitle = result?.name.toString()
            val placeAddress = result?.formatted_address.toString()
            val lat = result?.geometry?.location?.lat
            val lng = result?.geometry?.location?.lng
            val latLng: LatLng = LatLng(lat!!, lng!!)
            messageLocation = "${latLng.toString()} & \n$placeTitle & \n$placeAddress"
            Log.d("FetchedLocationFromMapOnCreate", messageLocation.toString())
            binding.messageInput.setText(" ")
            binding.messageInput.setText(messageLocation)
        }

        // GETTING MAPS DATA WITH INTERFACE


        // SETTING UP RECORDING
        mediaRecorder = MediaRecorder()
        binding.recordButton.setRecordView(binding.recordView)
        binding.recordButton.isListenForRecord = false

        binding.recordButton.setOnClickListener { view ->

            if (!isRecordPermissionGranted || !isWriteExternalPermissionGranted) {
                ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_CODE)
                mediaRecorder = MediaRecorder()

            } else {
                binding.recordButton.isListenForRecord = true

            }

            binding.recordView.setOnRecordListener(object : OnRecordListener {
                override fun onStart() {
                    //Start Recording..
                    Log.d("RecordView", "onStart")
                    if (!isRecordPermissionGranted || !isWriteExternalPermissionGranted) {
                        ActivityCompat.requestPermissions(requireActivity(),
                            permissions,
                            REQUEST_CODE)


                    } else {
                        mediaRecorder = MediaRecorder()
                        // createRecordingFile()
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // NEED TO CHECK AT RUN TIME

                        val file = File(Environment.getExternalStorageDirectory().absolutePath,
                            "chatapp_chatify")
                        if (!file.exists()) {
                            file.mkdirs()
                            audioPath =
                                file.absolutePath + File.separator + System.currentTimeMillis() + ".3gp"
                            mediaRecorder.setOutputFile(audioPath)
                            try {
                                mediaRecorder.prepare()
                                mediaRecorder.start()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            binding.messageInput.visibility = GONE
                            binding.attachmentCamera.visibility = GONE
                            binding.recordLayout.visibility = VISIBLE
                            binding.recordView.visibility = VISIBLE
                            binding.recordButton.visibility = VISIBLE
                        } else {
                            file.delete()
                            file.mkdirs()
                            audioPath =
                                file.absolutePath + File.separator + System.currentTimeMillis() + ".3gp"
                            mediaRecorder.setOutputFile(audioPath)
                            try {
                                mediaRecorder.prepare()
                                mediaRecorder.start()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            binding.messageInput.visibility = GONE
                            binding.attachmentCamera.visibility = GONE
                            binding.recordLayout.visibility = VISIBLE
                            binding.recordView.visibility = VISIBLE
                            binding.recordButton.visibility = VISIBLE
                        }


                    }
                }

                override fun onCancel() {
                    //On Swipe To Cancel
                    Log.d("RecordView", "onCancel")
                    try {
                        mediaRecorder.reset()
                        mediaRecorder.release()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }


                    if (audioPath != null) {
                        val file = File(audioPath!!)
                        if (file.exists()) {
                            file.delete()
                        }
                    }

                    binding.recordLayout.visibility = GONE
                    binding.messageInput.visibility = VISIBLE
                    binding.attachmentCamera.visibility = VISIBLE
                    binding.chatBottomLayout.visibility = VISIBLE


                }

                override fun onFinish(recordTime: Long, limitReached: Boolean) {
                    //Stop Recording..
                    //limitReached to determine if the Record was finished when time limit reached.
                    binding.recordView.visibility = GONE
                    binding.messageInput.visibility = View.VISIBLE
                    binding.chatBottomLayout.visibility = VISIBLE
                    binding.attachmentCamera.visibility = VISIBLE
                    binding.chatBottomLayout.visibility = VISIBLE

                    Log.d("RecordView", "onFinish")
                    try {
                        mediaRecorder.reset()
                        mediaRecorder.release()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }


                    // upload file to firebase Storage
                    sendRecordingFile(senderId)


                }

                override fun onLessThanSecond() {
                    //When the record time is less than One Second
                    Log.d("RecordView", "onLessThanSecond")
                    try {
                        mediaRecorder.reset()
                        mediaRecorder.release()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    if (audioPath != null) {
                        val file = File(audioPath!!)
                        if (file.exists()) {
                            file.delete()
                        }
                    }

                    binding.recordView.visibility = GONE
                    binding.chatBottomLayout.visibility = VISIBLE
                    binding.attachmentCamera.visibility = VISIBLE
                    binding.chatBottomLayout.visibility = VISIBLE
                }

                override fun onLock() {
                    //When Lock gets activated
                    Log.d("RecordView", "onLock")
                    binding.chatBottomLayout.visibility = GONE
                    binding.recordView.visibility = VISIBLE
                    binding.recordButton.visibility = VISIBLE
                }
            })

        }


        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            isEnabled = true
            if (binding.attachmentPopup.visibility == VISIBLE || binding.sendImageLayout.visibility == VISIBLE) {
                binding.attachmentPopup.visibility = GONE
                binding.sendImageLayout.visibility = GONE
                clickedImageUri = null
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }


        }



        mapsFragment?.setOnLocationSelectedListener(object : OnLocationSelectedListener {
            override fun onLocationSelected(location: Result) {
                val placeTitle = location.name.toString()
                val placeAddress = location.formatted_address.toString()
//                messageLocation = "${latLng.toString()} & \n$placeTitle & \n$placeAddress"
//                Log.d("FetchedLocationFromMapOnCreate",messageLocation.toString())
//                binding.messageInput.setText(" ")
//                binding.messageInput.setText(messageLocation)
            }

        })

        binding.sendVideoButton.setOnClickListener {
            Snackbar.make(requireView(),
                "Video Sharing Currently Unavailable",
                Snackbar.LENGTH_SHORT).show()
        }

        return binding.root

    }

    private fun sendImageMessage(senderId: String, messageText: String) {

        val randomKey = database.reference.push().key.toString()
        val lastMessageData: HashMap<String, Any> = HashMap<String, Any>()
        val timestamp = Calendar.getInstance().time.time

        val data = MessagesModel(randomKey,
            messageText,
            senderId,
            timestamp,
            6,
            senderRoom,
            Constant.MESSAGE_TYPE_IMAGE)

        lastMessageData["LastMessage"] = "Photo"
        lastMessageData["LastMessageTime"] = data.timestamp!!

        messageViewModel.sendMessage(senderRoom.toString(),
            receiverRoom.toString(),
            randomKey,
            lastMessageData,
            data,
            requireContext(),
            currentUser.token,
            userManager.getUserName())
    }


    private fun sendTextMessage(senderId: String, messageText: String, MESSAGE_TYPE_TEXT: Int) {

        val randomKey = database.reference.push().key.toString()
        val lastMessageData: HashMap<String, Any> = HashMap<String, Any>()

        if (binding.messageInput.text.isNotEmpty()) {

            if (MESSAGE_TYPE_TEXT != Constant.MESSAGE_TYPE_LOCATION) {
                val timestamp = Calendar.getInstance().time.time
                val data = MessagesModel(randomKey,
                    messageText,
                    senderId,
                    timestamp,
                    6,
                    senderRoom,
                    Constant.MESSAGE_TYPE_TEXT)
                binding.messageInput.setText("")

                lastMessageData["LastMessage"] = messageText
                lastMessageData["LastMessageTime"] = timestamp

                // send message to firebase
                messageViewModel.sendMessage(senderRoom.toString(),
                    receiverRoom.toString(),
                    randomKey,
                    lastMessageData,
                    data,
                    requireContext(),
                    currentUser.token,
                    userManager.getUserName())
            } else {
                sendLocationMessage(randomKey, senderId, messageText)
            }
        }
        //val messageText = binding.messageInput.text.toString()


    }

    private fun sendLocationMessage(randomKey: String, senderId: String, messageText: String) {
        val lastMessageData: HashMap<String, Any> = HashMap<String, Any>()
        val timestamp = Calendar.getInstance().time.time
        val data = MessagesModel(randomKey,
            messageText,
            senderId,
            timestamp,
            6,
            senderRoom,
            Constant.MESSAGE_TYPE_LOCATION)
        binding.messageInput.setText("")

        lastMessageData["LastMessage"] = "\uD83D\uDCCD Location"
        lastMessageData["LastMessageTime"] = timestamp

        // send message to firebase
        messageViewModel.sendMessage(senderRoom.toString(),
            receiverRoom.toString(),
            randomKey,
            lastMessageData,
            data,
            requireContext(),
            currentUser.token,
            userManager.getUserName())
    }

    private fun createImageUri(): Uri? {
        val image = File(requireActivity().applicationContext.filesDir, "chatify_photo.png")
        return FileProvider.getUriForFile(requireActivity().applicationContext,
            "com.example.chatapp_chatify.fileProvider",
            image)
    }

    private fun createRecordingFile() {

    }

    private fun sendRecordingFile(senderId: String) {
        val audioFile = Uri.fromFile(File(audioPath))
        var audioUrl: String = ""


        val myRef = firebaseStorage.reference.child("AudioRecordings/$audioPath")

        val uploadTask = myRef.putFile(audioFile)

        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            myRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.d("Audio Uploaded", downloadUri.toString())
                // URI successfully received now upload it to Firebase

                val randomKey = database.reference.push().key.toString()
                val lastMessageData: HashMap<String, Any> = HashMap<String, Any>()
                val timestamp = Calendar.getInstance().time.time

                val data = MessagesModel(randomKey,
                    downloadUri.toString(),
                    senderId,
                    timestamp,
                    6,
                    senderRoom,
                    Constant.MESSAGE_TYPE_AUDIO)

                lastMessageData["LastMessage"] = "Audio"
                lastMessageData["LastMessageTime"] = timestamp
                messageViewModel.sendMessage(senderRoom.toString(),
                    receiverRoom.toString(),
                    randomKey,
                    lastMessageData,
                    data,
                    requireContext(),
                    currentUser.token, userManager.getUserName())

            } else {
                Log.d("failed audio upload", "Audio upload failed")
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val currentUserId = auth.currentUser?.uid
        Log.d("FetchedLocationFromMap", messageLocation.toString())
        database.reference.child("presence").child(currentUserId.toString())
            .setValue(Constant.USER_ONLINE)
    }

    override fun onPause() {
        super.onPause()
        messageViewModel.addPreviousMessagesToLocalCache()
        val currentUserId = auth.currentUser?.uid
        database.reference.child("presence").child(currentUserId.toString())
            .setValue(Constant.USER_OFFLINE)
    }

    override fun onStop() {
        val currentUserId = auth.currentUser?.uid
        database.reference.child("presence").child(currentUserId.toString())
            .setValue(Constant.USER_OFFLINE)
        super.onStop()
    }

    override fun onDestroyView() {
        messageViewModel.addPreviousMessagesToLocalCache()
        super.onDestroyView()
    }

    fun sendNotificationsWithVolley(name: String, message: String, token: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "https://fcm.googleapis.com/fcm/send"
        val data = JSONObject()
        data.put("title", name)
        data.put("body", message)

        val notificationData = JSONObject()
        notificationData.put("notification", data)
        notificationData.put("to", data)


        val request: JsonObjectRequest =
            object : JsonObjectRequest(url, notificationData, Response.Listener {
                // Toast.makeText(ChatActivity.this, "success", Toast.LENGTH_SHORT).show();
            },
                Response.ErrorListener { error ->
                    Toast.makeText(requireContext(),
                        error.localizedMessage,
                        Toast.LENGTH_SHORT).show()
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val map: HashMap<String, String> = HashMap()
                    val key =
                        "key=AAAAWPeoLoE:APA91bGbmASsFPDVR3T47jKtHyug_GhIfbx15dJWp_y-q2yWFLGUZtmRALpl88VfWZ7KEdYJfM6W-jG6gW_yYTNZ7rTgOdr4juv3xLpnnfRpd4-q0--vrIAQIIqqZd6XoyAQY3rgeguH"
                    map["Content-Type"] = "application/json"
                    map["Authorization"] = key
                    return map
                }
            }
        queue.add(request)
    }


}