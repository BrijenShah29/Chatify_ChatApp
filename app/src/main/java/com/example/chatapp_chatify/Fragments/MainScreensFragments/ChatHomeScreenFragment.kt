package com.example.chatapp_chatify.Fragments.MainScreensFragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.chatapp_chatify.Adapters.ChatListAdapter
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import com.example.chatapp_chatify.databinding.FragmentChatScreenBinding
import com.example.chatapp_chatify.utils.UserManager
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatScreenBinding.inflate(layoutInflater)

        val adapter = ChatListAdapter(requireContext(),userManager,auth,db,messageViewModel)
        binding.chatListRecycler.adapter = adapter

        viewModel.getUserProfilesFromFirestore("Users")

        viewModel.fetchedAllUsersList.observe(viewLifecycleOwner, Observer {
            val data = ArrayList<Users>()
            for(each in it)
            {
                if(each.uid != auth.currentUser?.uid.toString()){
                    data.add(each)
                }
            }
            adapter.submitList(data)
            adapter.notifyDataSetChanged()
            //Log.d("FetchedUsersMain", it[0].name!!)
        })


        return binding.root
    }


}