package com.example.chatapp_chatify.Fragments.MainScreensFragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.FragmentSettingsBinding
import com.example.chatapp_chatify.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

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

    lateinit var binding : FragmentSettingsBinding

    @Inject
    lateinit var userManger : UserManager

    private var coverImage: Uri? = null
    private var coverImageUrl: String? = ""

    @Inject
    lateinit var auth : FirebaseAuth

    private var launchGalleryForActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode == Activity.RESULT_OK){
            coverImage = it.data!!.data
            binding.profilePicture.borderColor = resources.getColor(R.color.purple_500)
            binding.profilePicture.setImageURI(coverImage)
            binding.profilePicture.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(layoutInflater)

        binding.nameEdittext.setText(userManger.getUserName())
        Glide.with(requireContext()).load(userManger.getUserProfileImage()).centerCrop().into(binding.profilePicture)
        binding.registeredNumberTextview.text = String.format(getString(R.string.registered_number),userManger.getUserNumber())



        
        


        binding.profilePicture.setOnClickListener {
            val alert = AlertDialog.Builder(requireContext()).setMessage("Update profile picture ?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->

                    val intent = Intent("android.intent.action.GET_CONTENT")
                    intent.type = "image/*"
                    launchGalleryForActivity.launch(intent)
                }
                .setNegativeButton("No"){dialog,id->
                    dialog.dismiss()
                }
            alert.create().show()
        }
        binding.editNameButton.setOnClickListener {
            binding.nameEdittext.isEnabled = true
            binding.nameEdittext.requestFocus()

        }

        binding.signOutButton.setOnClickListener {
            auth.signOut()
            // navigate to welcome page , remove all previous fragment backlog

            findNavController().navigate(R.id.welcomePage,null,
                NavOptions.Builder().setPopUpTo(R.id.chatScreenFragment, true).build())
        }
        
        
        
        
        
        return binding.root
    }



}