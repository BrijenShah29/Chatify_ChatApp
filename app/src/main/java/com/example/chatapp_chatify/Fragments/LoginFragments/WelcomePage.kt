package com.example.chatapp_chatify.Fragments.LoginFragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.FragmentWelcomePageBinding
import com.google.firebase.auth.FirebaseAuth



class WelcomePage : Fragment() {


    lateinit var binding : FragmentWelcomePageBinding

    lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWelcomePageBinding.inflate(layoutInflater)

        auth = FirebaseAuth.getInstance()

        binding.getStartedButton.setOnClickListener{

            Navigation.findNavController(requireView()).navigate(R.id.action_welcomePage_to_authenticationFragment)

        }









        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser!=null)
        {
            Navigation.findNavController(requireView()).navigate(R.id.action_welcomePage_to_chatScreenFragment)
        }
    }


}