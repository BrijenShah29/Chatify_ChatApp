package com.example.chatapp_chatify.Fragments.LoginFragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.example.chatapp_chatify.databinding.FragmentEnterOTPBinding
import com.example.chatapp_chatify.utils.UserManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EnterOTPFragment : Fragment() {


    lateinit var binding : FragmentEnterOTPBinding
    private var verificationCode: String? = ""
    private lateinit var userNumber: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    val viewModel by viewModels<FirebaseViewModel>()

    private lateinit var mProgressBar: ProgressBar

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEnterOTPBinding.inflate(layoutInflater)


        verificationCode = requireArguments().getString("verificationID")?.toString()
        userNumber = requireArguments().getString("number").toString()
        resendToken = requireArguments().getParcelable("resendToken")!!

        // SETTING UP PHONE NUMBER TEXT FIELD

        binding.phoneNumber.text = userNumber

        var spanString = SpannableString(binding.phoneNumber.text.toString())
        spanString.setSpan(UnderlineSpan(),
            0,
            binding.phoneNumber.text.lastIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.phoneNumber.text = spanString

        mProgressBar = binding.otpProgressBar
        mProgressBar.visibility = View.INVISIBLE

        binding.otpView.requestFocus()



        // SETTING UP VERIFY BUTTON
        binding.getStartedButton.setOnClickListener {
            verifyButtonOnclick(binding.otpView.text.toString())
        }


        // SETTING UP OTP AUTO COMPLETE
//
//        binding.otpView.setOtpCompletionListener {
//            verifyButtonOnclick(it)
//        }


        // HIDING OTP RESENT FUNCTIONALITY FOR 60 SECONDS AT START

        getResendOTPVisibility()


        // SETTING ONCLICK FOR CHANGE OF NUMBER
        binding.phoneNumber.setOnClickListener {
            requireActivity().onBackPressed()
        }


        //SETTING RESENT OTP ONCLICK

        binding.resendOtpButton.setOnClickListener {

            mProgressBar.visibility = View.VISIBLE

            getResendOTPVisibility()
            resendVerificationCode()
        }



        return binding.root
    }

    private fun verifyButtonOnclick(code: String) {

        if(code.isNotEmpty()){
            if(code.length == 6){

                mProgressBar.visibility = View.VISIBLE

                val credential = PhoneAuthProvider.getCredential(verificationCode!!,binding.otpView.text.toString())
                signInWithPhoneAuthCredential(credential)
            }else
            {
                Snackbar.make(requireView(), "Please Enter Correct OTP", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }else
        {
            Snackbar.make(requireView(), "Please Enter OTP", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getResendOTPVisibility() {

        binding.otpView.text = null
        binding.resendOtpButton.visibility = View.GONE
        binding.resendOtpTxt.visibility = View.GONE

        Handler(Looper.myLooper()!!).postDelayed(Runnable {
            binding.resendOtpButton.visibility = View.VISIBLE
            binding.resendOtpTxt.visibility = View.VISIBLE
        }, 60000)

    }

    private fun resendVerificationCode() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(userNumber)
            .setTimeout(60L,TimeUnit.SECONDS)
            .setActivity(this.requireActivity())
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks =  object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {

            Log.d(TAG, "onVerificationCompleted:$credential")

            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(p0: FirebaseException) {

            if (p0 is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                Toast.makeText(requireContext(), "Invalid Request", Toast.LENGTH_SHORT).show()
            } else if (p0 is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                Toast.makeText(requireContext(), "We're facing too many requests right now , Please try again later", Toast.LENGTH_LONG).show()
            }
            mProgressBar.visibility = View.INVISIBLE
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            mProgressBar.visibility = View.INVISIBLE
            Snackbar.make(requireView(), "OTP Sent Successfully", Snackbar.LENGTH_SHORT).show()
            verificationCode = verificationId
            resendToken = token
        }

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

        auth.signInWithCredential(credential).addOnCompleteListener {
            task ->

            if(task.isSuccessful)
            {
                Toast.makeText(requireContext(), "success! Please Wait", Toast.LENGTH_SHORT).show()
                val user = task.result.user
                sendToMainScreen(user)
            }
            else
            {
                Log.w("OTP FRAGMENT", "signInWithCredential:failure", task.exception)

                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    mProgressBar.visibility = GONE
                    Snackbar.make(requireView(),"Invalid OTP, please retry",Snackbar.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun sendToMainScreen(user: FirebaseUser?) {

        Toast.makeText(requireContext(), "User $user logged in successfully", Toast.LENGTH_SHORT).show()

        // STORE USER NUMBER INTO SHARED PREFERENCES
        userManager.savePhoneNumber(user!!.phoneNumber.toString())



        // CHECK IF USER EXISTS IN DATABASE IF YES REDIRECT TO MAIN PAGE , IF NOT SEND USER TO REGISTER PAGE

        viewModel.fetchUserProfileFromFirebase("Users",user.phoneNumber.toString())

        viewModel.fetchedUserProfile.observe(viewLifecycleOwner, Observer {
            Handler(Looper.myLooper()!!).postDelayed(Runnable {
                if (it != null && it.uid.isNotBlank() && it.uid.isNotEmpty()) {
                    //transfer user to main screen with data bundle and also store details into shared pref

                    userManager.savePhoneNumber(it.phoneNumber.toString())
                    userManager.saveUserName(it.name.toString())
                    userManager.saveUserImage(it.profileImage.toString())
                    userManager.saveUserId(auth.currentUser?.uid.toString())

//                        val bundle = Bundle()
//                        bundle.putParcelable("UserData", it)
                    mProgressBar.visibility = View.INVISIBLE
                    Navigation.findNavController(requireView())
                        .navigate(R.id.action_enterOTPFragment_to_chatScreenFragment)
                } else {

                    val bundle = Bundle()
                    bundle.putString("userNumber", userNumber)
                    Navigation.findNavController(requireView())
                        .navigate(R.id.action_enterOTPFragment_to_registerationFragment,
                            bundle)
                    mProgressBar.visibility = View.INVISIBLE

                }
            }, 2000)
            // }
            //  },2000)

        })
    }


}