package com.example.chatapp_chatify.Fragments.LoginFragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseViewModel
import com.example.chatapp_chatify.databinding.FragmentAuthenticationBinding
import com.example.chatapp_chatify.utils.UserManager
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.managers.FragmentComponentManager
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticationFragment : Fragment() {

    lateinit var binding : FragmentAuthenticationBinding

    private val viewModel by viewModels<FirebaseViewModel>()

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var auth : FirebaseAuth

    private lateinit var mProgressBar: ProgressBar
    private var userNumber: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAuthenticationBinding.inflate(layoutInflater)

        auth = FirebaseAuth.getInstance()
        mProgressBar = binding.otpProgressBar
        mProgressBar.visibility = View.INVISIBLE

        val number = binding.ccp
        number.registerCarrierNumberEditText(binding.phoneInputTxt)

        binding.getStartedButton.setOnClickListener {
            if(Pattern.matches("^(\\+\\d{1,2}\\s?)?1?\\-?\\.?\\s?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}\$",number.fullNumber))
            {
               // auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
                mProgressBar.visibility = View.VISIBLE
                userNumber = "+"+number.fullNumber.toString()
                Log.d("phoneNumber", userNumber.toString())
                verifyUserAuth(userNumber.toString())

            }
        }


        return binding.root
    }

    private fun verifyUserAuth(userNumber: String) {

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(userNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this.requireActivity())
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

    }
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredentials(credential)
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

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken)
        {
           val bundle = Bundle()
            bundle.putString("verificationID", verificationId)
            bundle.putParcelable("resendToken", token)
            bundle.putString("number", userNumber.toString())
            mProgressBar.visibility = View.INVISIBLE

            Navigation.findNavController(requireView()).navigate(R.id.action_authenticationFragment_to_enterOTPFragment,bundle)
        }
    }

    private fun signInWithPhoneAuthCredentials(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()){
                task ->
                if(task.isSuccessful)
                {
                    val user = task.result.user
                    sendToMainScreen(user)
                }

            }

    }

    private fun sendToMainScreen(user: FirebaseUser?) {

        // ADD USER INTO SHARED PREFERENCES AND THEN NAVIGATE TO MAIN SCREEN
        userManager.savePhoneNumber(user?.phoneNumber.toString())

        Navigation.findNavController(requireView())
            .navigate(R.id.action_authenticationFragment_to_chatScreenFragment)



    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser!=null){
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_authenticationFragment_to_chatScreenFragment)

            }
        }


}