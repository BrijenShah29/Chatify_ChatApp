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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.devlomi.record_view.OnRecordListener
import com.example.chatapp_chatify.Adapters.MessagesAdapter
import com.example.chatapp_chatify.DataClass.MessagesModel
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import com.example.chatapp_chatify.databinding.FragmentUserChatBinding
import com.example.chatapp_chatify.utils.UserManager
import com.github.pgreze.reactions.dsl.reactionConfig
import com.github.pgreze.reactions.dsl.reactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject


@SuppressLint("SuspiciousIndentation")
@AndroidEntryPoint
class UserChatFragment : Fragment() {


    private var isRecordPermissionGranted = false

    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    val REQUEST_CODE = 111
    private lateinit var permissionLauncher : ActivityResultLauncher<Array<String>>

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
    lateinit var firebaseStorage : FirebaseStorage

    private var senderRoom: String? = ""
    private var receiverRoom: String? = ""

    lateinit var messageList: List<MessagesModel>

    private val viewModel by viewModels<FirebaseViewModel>()
    private val messageViewModel by viewModels<FirebaseMessagesViewModel>()
    private var listening : Boolean = false

    private lateinit var mediaRecorder : MediaRecorder
    private var audioPath : String ? = ""


    private var clickedImageUri : Uri? = null
    private var imageUri : Uri? = null

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(it){
            binding.imagePreview.setImageURI(null)
            binding.imagePreview.setImageURI(clickedImageUri)
            binding.sendImageLayout.visibility = View.VISIBLE
            binding.cancelImage.visibility = View.VISIBLE
        }else{
            binding.imagePreview.setImageURI(null)
            binding.sendImageLayout.visibility = View.GONE
            binding.cancelImage.visibility = View.GONE
        }

    }
    private var launchGalleryForActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode == Activity.RESULT_OK){
            clickedImageUri = it.data!!.data
            binding.imagePreview.setImageURI(clickedImageUri)
            binding.sendImageLayout.visibility = View.VISIBLE
            binding.cancelImage.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE){
            isRecordPermissionGranted = grantResults[0] ==PackageManager.PERMISSION_GRANTED
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

        if(requireArguments().getString("senderRoom")!=null){
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
        }

    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUserChatBinding.inflate(layoutInflater)

        val actionbar = (requireActivity() as AppCompatActivity).supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)

        binding.attachmentPopup.visibility = View.GONE
        binding.sendImageLayout.visibility = View.GONE
        binding.cancelImage.visibility = View.GONE

        messageList = ArrayList()

        isRecordPermissionGranted = ActivityCompat.checkSelfPermission(requireContext(),permissions[0]) == PackageManager.PERMISSION_GRANTED
        if(!isRecordPermissionGranted){
            ActivityCompat.requestPermissions(requireActivity(),permissions,REQUEST_CODE)
        }


        val senderId = auth.currentUser?.uid.toString()
        val receiverId = userManager.getOppositeUserId().toString()
//        val senderNumber = userManager.getUserNumber().toString()
//        val receiverNumber = userManager.getOppositeUserNumber().toString()


        val db = database.reference.child("Chats")
        senderRoom = senderId.toString() + receiverId.toString()
        receiverRoom = receiverId.toString() + senderId.toString()

        // START LISTENING TO CHAT
        if(!listening){
            messageViewModel.startListeningToMessages(senderRoom!!)
        }


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
            if(clickedImageUri!=null){
                contract.launch(clickedImageUri)
            }else
            {
                clickedImageUri = createImageUri()
                contract.launch(clickedImageUri)
            }

        }

        binding.cancelImage.setOnClickListener {
            Log.d("cancel image","cancel image clicked")
            clickedImageUri = null
            Toast.makeText(requireContext(), "Image Removed.", Toast.LENGTH_SHORT).show()
            binding.imagePreview.setImageURI(null)
            binding.sendImageLayout.visibility = GONE
            binding.cancelImage.visibility = View.GONE
        }

        //GETTING DATA FROM LOCAL CACHE FIRST
        messageViewModel.fetchMessagesForOffLine(senderRoom!!)
        messageViewModel.fetchedLocalCacheMessages.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(!it.isNullOrEmpty())
            {
                messageList = it
            }
            })


        //SETTING UP THE RECYCLERVIEW AND ADAPTER
       // messageViewModel.getMessages(senderRoom!!)
        adapter = MessagesAdapter(requireContext(), senderRoom!!, receiverRoom!!, db, config,auth)
        binding.chatRecyclerview.adapter = adapter

            // GETTING DATA FROM FIREBASE IF THERE'S ANY UPDATE IN MESSAGE
        messageViewModel.startListeningToMessages.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
               if(it.isNullOrEmpty()){
                   adapter.submitList(messageList)
                   adapter.notifyDataSetChanged()
               }else {
                adapter.submitList(it.toList())
            Log.d("receivedChatMain",it.get(0).toString())
                adapter.notifyDataSetChanged()

               }
        })

        binding.sendButton.setOnClickListener {
            sendMessage(db, senderId)
        }

        binding.attachmentButton.setOnClickListener {
            if (binding.attachmentPopup.visibility == View.GONE || binding.attachmentPopup.visibility == View.INVISIBLE) {
                binding.attachmentPopup.visibility = View.VISIBLE
            } else {
                binding.attachmentPopup.visibility = View.GONE
            }
        }

        // SETTING UP TEXT WATCHER

        binding.messageInput.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p0.toString().trim().isNotEmpty()) {

                    binding.chatBottomLayout.visibility = View.VISIBLE
                    binding.recordButton.visibility = View.GONE
                    binding.sendButton.visibility - View.VISIBLE
                }
                else
                {
                    binding.chatBottomLayout.visibility = View.VISIBLE
                    binding.recordButton.visibility = View.VISIBLE
                    binding.sendButton.visibility - View.GONE
                }
            }

            override fun afterTextChanged(p0: Editable?) {

                if(p0.toString().trim().isNotEmpty()) {

                    binding.chatBottomLayout.visibility = View.VISIBLE
                    binding.recordButton.visibility = View.GONE
                    binding.sendButton.visibility - View.VISIBLE
                }
                else
                {
                    binding.chatBottomLayout.visibility = View.VISIBLE
                    binding.recordButton.visibility = View.VISIBLE
                    binding.sendButton.visibility - View.GONE
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




        // SETTING UP RECORDING
        mediaRecorder = MediaRecorder()
        binding.recordButton.setRecordView(binding.recordView)
        binding.recordButton.isListenForRecord = false

        binding.recordButton.setOnClickListener { view ->

           if(!isRecordPermissionGranted){
               ActivityCompat.requestPermissions(requireActivity(),permissions,REQUEST_CODE)
               mediaRecorder = MediaRecorder()

           }else
           {
               binding.recordButton.isListenForRecord = true

           }

            binding.recordView.setOnRecordListener(object : OnRecordListener {
                override fun onStart() {
                    //Start Recording..
                    Log.d("RecordView", "onStart")
                    if (!isRecordPermissionGranted) {
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
//                        if (!file.exists()) {
//                            file.mkdirs()
                            audioPath =
                                file.absolutePath + File.separator + System.currentTimeMillis() + ".3gp"
                            mediaRecorder.setOutputFile(audioPath)
                        try {
                            mediaRecorder.prepare()
                            mediaRecorder.start()
                        }
                        catch (e: IOException) {
                                e.printStackTrace()
                            }

                            binding.chatBottomLayout.visibility = View.GONE
                            binding.recordView.visibility = View.VISIBLE
                            binding.recordButton.visibility = View.VISIBLE
                       // }
//                        else.
//                        {
//                            file.delete()
//                            file.mkdirs()
//                            audioPath = file.absolutePath+File.separator+System.currentTimeMillis()+".3gp"
//                            mediaRecorder!!.setOutputFile(audioPath)
//                            mediaRecorder?.prepare()
//                            mediaRecorder?.start()
//                            binding.chatBottomLayout.visibility = View.GONE
//                            binding.recordView.visibility = View.VISIBLE
//                            binding.recordButton.visibility = View.VISIBLE
//                        }


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

                    binding.recordView.visibility = View.GONE
                    binding.chatBottomLayout.visibility = View.VISIBLE


                }

                override fun onFinish(recordTime: Long, limitReached: Boolean) {
                    //Stop Recording..
                    //limitReached to determine if the Record was finished when time limit reached.

                    Log.d("RecordView", "onFinish")
                    try {


                        mediaRecorder.reset()
                        mediaRecorder.release()
                    }
                     catch (e: IOException) {
                        e.printStackTrace()
                    }
                    binding.recordView.visibility = View.GONE
                    binding.chatBottomLayout.visibility = View.VISIBLE

                    // upload file to firebase Storage
                    if (!audioPath.isNullOrEmpty()) {
                        sendRecordingFile(audioPath!!)
                    }


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

                    binding.recordView.visibility = View.GONE
                    binding.chatBottomLayout.visibility = View.VISIBLE
                }

                override fun onLock() {
                    //When Lock gets activated
                    Log.d("RecordView", "onLock")
                    binding.chatBottomLayout.visibility = View.GONE
                    binding.recordView.visibility = View.VISIBLE
                    binding.recordButton.visibility = View.VISIBLE


                }
            })

        }




        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            isEnabled = true
            if (binding.attachmentPopup.visibility == View.VISIBLE || binding.sendImageLayout.visibility==View.VISIBLE) {
                binding.attachmentPopup.visibility = View.GONE
                binding.sendImageLayout.visibility = View.GONE
                clickedImageUri = null
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }


        }




        return binding.root

    }


    private fun sendMessage(db: DatabaseReference, senderId: String) {

        val randomKey = database.reference.push().key.toString()
        val lastMessageData: HashMap<String, Any> = HashMap<String, Any>()

        if (binding.messageInput.text.isNotEmpty()) {
            val messageText = binding.messageInput.text.toString()
            val timestamp = Calendar.getInstance().time.time
            val data = MessagesModel(randomKey, messageText, senderId, timestamp, 6,senderRoom)

            binding.messageInput.setText("")

            lastMessageData.put("LastMessage",messageText)
            lastMessageData.put("LastMessageTime", timestamp)


            // send message to firebase
            messageViewModel.sendMessage(senderRoom.toString(), receiverRoom.toString(),randomKey,lastMessageData, data,requireContext())
                }

        }

    private fun createImageUri(): Uri? {
        val image = File(requireActivity().applicationContext.filesDir,"chatify_photo.png")
        return FileProvider.getUriForFile(requireActivity().applicationContext,
            "com.example.chatapp_chatify.fileProvider",
        image)
    }

    private fun createRecordingFile()
    {

    }

    private fun sendRecordingFile(audioPathToSend : String) {
        val audioFile = Uri.fromFile(File(audioPath))
        var audioUrl: String = ""


//        messageViewModel.addRecordingMessageToFirebaseStorage(audioFile,audioPath!!,"AudioRecordings")
//      messageViewModel.recodingMessageUrl.observe(viewLifecycleOwner, androidx.lifecycle.Observer { path->
//          if(!path.isNullOrEmpty()){
//               val audioUrl = path.toString()
//               Log.d("audio uploaded",path.toString())
////                // Send Path as a Message in Firebase Database
//          }
//        })

        val myRef = firebaseStorage.reference.child("AudioRecordings/$audioPath")

        val uploadTask = myRef.putFile(audioFile)

        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            myRef.downloadUrl }.addOnCompleteListener {
                task->
            if(task.isSuccessful){
                val downloadUri = task.result.path
                Log.d("Audio Uploaded",downloadUri.toString())
                // URI successfully received now upload it to Firebase



            }else
            {
                Log.d("failed audio upload","Audio upload failed")
            }
        }
//***************************************************************************************************************
//                myRef.downloadUrl
//                    .addOnSuccessListener {
//                        audioUrl = it.toString()
//                    }
//                    .addOnFailureListener {
//
//                        Log.d("failed audio path","Audio path fetching failed")
//
//                    }
//                    .addOnFailureListener {
//                        Log.d("failed audio upload","Audio upload failed")
//                    }
    }

        override fun onPause() {
        super.onPause()
        messageViewModel.addPreviousMessagesToLocalCache()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageViewModel.addPreviousMessagesToLocalCache()
    }

    }