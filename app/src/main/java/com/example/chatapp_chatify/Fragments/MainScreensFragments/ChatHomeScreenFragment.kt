package com.example.chatapp_chatify.Fragments.MainScreensFragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.chatapp_chatify.Adapters.ChatListAdapter
import com.example.chatapp_chatify.CallingActivities.AudioConferenceActivity
import com.example.chatapp_chatify.CallingActivities.VideoConferenceActivity
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import com.example.chatapp_chatify.databinding.FragmentChatScreenBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.UserManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatHomeScreenFragment : Fragment() {

    lateinit var binding : FragmentChatScreenBinding

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var auth : FirebaseAuth

    @Inject
    lateinit var db : FirebaseDatabase
    private var tokenChannel : List<String>? = null

    val viewModel by viewModels<FirebaseViewModel>()

    val messageViewModel by viewModels<FirebaseMessagesViewModel>()

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemAudio: MenuItem = menu.findItem(R.id.audioCall_appBar)
        val itemVideo: MenuItem = menu.findItem(R.id.videoCall_appBar)
        itemAudio.isVisible = false
        itemVideo.isVisible = false


    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.getUserProfilesFromFirestore("Users")
        messageViewModel.startListeningToCallingTokens(auth.currentUser?.uid.toString())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatScreenBinding.inflate(layoutInflater)

        val adapter = ChatListAdapter(requireContext(),userManager,auth,db,messageViewModel)

        viewModel.fetchedAllUsersList.observe(viewLifecycleOwner, Observer {
            val data = ArrayList<Users>()
            for(each in it)
            {
                if(each.uid != auth.currentUser?.uid.toString()){
                    data.add(each)
                }
            }
            binding.chatListRecycler.adapter = adapter
            adapter.submitList(data)
            adapter.notifyDataSetChanged()
            //adapter.notifyDataSetChanged()
            //Log.d("FetchedUsersMain", it[0].name!!)
        })

        //STORING NOTIFICATION TOKEN OF CURRENT USER
        messageViewModel.getUserTokenAndUpdateUserData()


        //GETTING CALLING TOKEN IF ANY
        messageViewModel.userCallingToken.observe(viewLifecycleOwner, Observer { token ->
            if(token.toString().isNotEmpty())
            {
                Log.d("Token",token.toString())
                if (token.split(",") != tokenChannel)
                {
                    goToNextActivity(token)
                }
            }
        })

        return binding.root
    }

    private fun goToNextActivity(token: String) {

        val tokenArray = token.toString().split(",")
        tokenChannel = tokenArray
            if(tokenArray.contains("AudioCallInvitation"))
            {
                Log.d("AudioCallDetected","Audio call detected")
                val intent = Intent(requireContext(),AudioConferenceActivity::class.java)
                intent.putExtra("incoming","incoming Call")
                intent.putExtra("channelToken",token)
                startActivity(intent)
                Snackbar.make(binding.root,"Incoming call, Please check notification",Snackbar.LENGTH_LONG).show()
            }
            else if(tokenArray.contains("VideoCallInvitation"))
            {
                Log.d("VideoCallDetected","Video call detected")
                val intent = Intent(requireContext(), VideoConferenceActivity::class.java)
                intent.putExtra("incoming","incoming Call")
                intent.putExtra("channelToken",token)
                startActivity(intent)
                Snackbar.make(binding.root,"Incoming call, Please check notification",Snackbar.LENGTH_LONG).show()
            }

    }

    override fun onResume() {
        super.onResume()
        val currentUserId = auth.currentUser?.uid
        messageViewModel.startListeningToCallingTokens(auth.currentUser?.uid.toString())
        db.reference.child("presence").child(currentUserId.toString()).setValue(Constant.USER_ONLINE)
    }

    override fun onDestroy() {
        super.onDestroy()
        Thread {
            val currentUserId = auth.currentUser?.uid
            db.reference.child("presence").child(currentUserId.toString())
                .setValue(Constant.USER_OFFLINE)
        }.start()
    }


}