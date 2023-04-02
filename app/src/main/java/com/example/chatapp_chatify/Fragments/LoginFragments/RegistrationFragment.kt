package com.example.chatapp_chatify.Fragments.LoginFragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import com.example.chatapp_chatify.databinding.FragmentRegisterationBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.UserManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject


@AndroidEntryPoint
class RegistrationFragment : Fragment() {


    lateinit var binding : FragmentRegisterationBinding

    var userNumber: String? = null
    var userName: String? = null
    private var coverImage: Uri? = null
    private var coverImageUrl: String? = ""

    private lateinit var mProgressBar: ProgressBar

    private val viewModel by viewModels<FirebaseViewModel>()

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var database : FirebaseDatabase

    @Inject
    lateinit var storage : FirebaseStorage

    @Inject
    lateinit var userManager: UserManager


    private var launchGalleryForActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode == Activity.RESULT_OK){
            coverImage = it.data!!.data
            binding.updateUserProfileImage.borderColor = resources.getColor(R.color.black)
            binding.updateUserProfileImage.setImageURI(coverImage)
            binding.updateUserProfileImage.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterationBinding.inflate(layoutInflater)


        // SETTING UP ONCLICK ON IMAGE
        binding.updateUserProfileImage.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            launchGalleryForActivity.launch(intent)
        }

        userNumber = requireArguments().getString("userNumber")

        binding.cellNumber.setText(userNumber)
        mProgressBar = binding.otpProgressBar

        binding.registerButton.setOnClickListener {

            if(!Pattern.matches("^[a-zA-Z]+(([\\'\\,\\.\\- ][a-zA-Z ])?[a-zA-Z]*)*\$",binding.firstName.text.toString()))
            {
                binding.firstName.error = "Please enter a valid name"
            }
            else
            {
                mProgressBar.visibility = View.VISIBLE
                Snackbar.make(requireView(),"Please wait while we setting up your profile", Snackbar.LENGTH_LONG).show()
                setupUserProfile()
            }
        }

        return binding.root
    }

    private fun setupUserProfile() {

        var data: Users? = null

        CoroutineScope(Dispatchers.Main).launch {
            // setup profile image first
            if (coverImage != null) {
                viewModel. uploadProfileImageToFirebaseStorage(coverImage!!,
                    userNumber!!,
                    "Users")
                viewModel.profileImageUploadedUrl.observe(viewLifecycleOwner, Observer {
                    userManager.saveUserImage(it)
                })
                delay(2000)

                if (userManager.getUserProfileImage() != Constant.USER_IMAGE_FILE) {
                    data = Users(
                        auth.uid.toString(),
                        binding.firstName.text.toString(),
                        userNumber!!,
                        userManager.getUserProfileImage().toString()
                    )
                }
            } else {
                data = Users(
                    auth.uid.toString(),
                    binding.firstName.text.toString(),
                    userNumber!!,
                )
            }
            // store it in users class and pass it on firebase

            if(data!=null){
                viewModel.uploadDataIntoFirebase(data!!, "Users")
            }


            viewModel.uploadStatus.observe(viewLifecycleOwner, Observer {

                if (it) {

                    mProgressBar.visibility = View.INVISIBLE
                    // STORE USER NAME INTO SHARED PREFERENCES
                    userManager.saveUserName(binding.firstName.text.toString())
                    userManager.savePhoneNumber(userNumber)
                    userManager.saveUserId(auth.currentUser?.uid.toString())

                    // NAVIGATE USER TO HOME SCREEN


                    findNavController().navigate(R.id.action_registerationFragment_to_chatScreenFragment,null,
                        NavOptions.Builder().setPopUpTo(R.id.welcomePage, true).build())
                    val nav = Navigation.findNavController(requireView())
                    //nav.navigate( R.id.action_registerationFragment_to_chatScreenFragment)
                } else {
                    mProgressBar.visibility = View.VISIBLE
                }

            })
            // on successful data transmit to database, Redirect User to Main screen
        }
    }
}