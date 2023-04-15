package com.example.chatapp_chatify.Fragments.MainScreensFragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.chatapp_chatify.Adapters.CallAdapter
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.databinding.FragmentCallsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallsFragment : Fragment() {

    lateinit var binding : FragmentCallsBinding

    val viewModel by viewModels<FirebaseMessagesViewModel>()
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
        binding = FragmentCallsBinding.inflate(layoutInflater)

        val adapter = CallAdapter(requireContext())
        binding.callLogRecycler.adapter = adapter
        viewModel.getCallLog()
        viewModel.fetchedCallLog.observe(viewLifecycleOwner, Observer {
            if(!it.isNullOrEmpty())
            {
                adapter.submitList(it)
            }

        })







        return binding.root
    }
}