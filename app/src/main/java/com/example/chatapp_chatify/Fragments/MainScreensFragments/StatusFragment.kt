package com.example.chatapp_chatify.Fragments.MainScreensFragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.VISIBLE
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.Adapters.StoriesAdapter
import com.example.chatapp_chatify.DataClass.StatusImages
import com.example.chatapp_chatify.DataClass.UserStatus
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.databinding.FragmentWebBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.UserManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class StatusFragment : Fragment() {

    private var coverImage: Uri? = null
    private var coverImageUrl: String? = ""
    lateinit var binding: FragmentWebBinding
    private lateinit var mProgressBar: ProgressBar

    private var previousStory = ""

    private val currentUserStory = kotlin.collections.ArrayList<UserStatus>()
    private lateinit var currentUserStoryImages: ArrayList<StatusImages>

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var auth: FirebaseAuth

    private val viewModel by viewModels<FirebaseMessagesViewModel>()

    override fun onStart() {
        super.onStart()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemAudio: MenuItem = menu.findItem(R.id.audioCall_appBar)
        val itemVideo: MenuItem = menu.findItem(R.id.videoCall_appBar)
        itemAudio.isVisible = false
        itemVideo.isVisible = false

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.startListeningToStories()
        currentUserStoryImages = ArrayList()
    }


    private var launchGalleryForActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            coverImage = it.data!!.data
            binding.userProfile.borderColor = resources.getColor(R.color.purple_500)
            binding.userProfile.setImageURI(coverImage)
            binding.userProfile.visibility = View.VISIBLE
            uploadStatusToFirebase(coverImage)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentWebBinding.inflate(layoutInflater)

        mProgressBar = binding.otpProgressBar

        val adapter = StoriesAdapter(requireContext(), userManager.getUserName())
        binding.storiesRecyclerView.adapter = adapter

        // setting up user profile image
        Glide.with(requireContext()).load(Uri.parse(userManager.getUserProfileImage())).centerCrop()
            .into(binding.userProfile)
        // LISTEN STATUSES FETCHED FROM DATABASE
        viewModel.startListeningToStories.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val data = kotlin.collections.ArrayList<UserStatus>()
            currentUserStory.clear()
            currentUserStoryImages.clear()
            if (it.isNotEmpty()) {
                // SEND LIST TO ADAPTER
                for (items in it) {
                    if (items.uploaderUid.toString() != auth.currentUser?.uid.toString()) {
                        data.add(items)
                    } else {
                        currentUserStory.add(items)
                    }
                }
                adapter.submitList(data)
                adapter.notifyDataSetChanged()

                for (items in currentUserStory) {
                    for (item in items.status) {
                        currentUserStoryImages.add(item)
                        Picasso.get().load(Uri.parse(item.imageUrl)).fit().centerCrop()
                            .into(binding.userProfile)
                        binding.userStatusCircles.setPortionsCount(currentUserStoryImages.size)
                        binding.userStatusCircles.setPortionsColor(ResourcesCompat.getColor(
                            requireContext().resources,
                            R.color.purple_200,
                            null))
                    }
                }

            }
        })


        binding.addStoryBtn.setOnClickListener {
            mProgressBar.visibility = View.VISIBLE
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            launchGalleryForActivity.launch(intent)
        }

        binding.userProfile.setOnClickListener {
            binding.userProfile.borderWidth = 1
            binding.userProfile.borderColor =
                ResourcesCompat.getColor(requireContext().resources, R.color.primary_color, null)
            val storyArray = java.util.ArrayList<MyStory>()
            var time: Long? = null
            var showTime: String? = null
            if (currentUserStoryImages.size >= 1) {
                storyArray.clear()
                for (data in currentUserStoryImages) {
                    storyArray.add(MyStory(data.imageUrl, Date(data.timeStamp!!)))
                    time = data.timeStamp

                }
                showStories(storyArray,
                    requireContext(),
                    currentUserStory[0].name!!,
                    currentUserStory[0].profileImage.toString(),
                    getTime(time!!))
            } else {
                Snackbar.make(requireView(), "No Story Uploaded !", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.deleteStoryImage.setOnClickListener {
            if (binding.deleteStoriesCardview.visibility == View.VISIBLE) {
                binding.deleteStoriesCardview.visibility = View.GONE
            } else {
                binding.deleteStoriesCardview.visibility = View.VISIBLE
            }
        }
        binding.deletePreviousStory.setOnClickListener {
            // DELETE LAST STORY FROM FIREBASE
            if (currentUserStory.size > 0) {
                viewModel.deleteLastStory()
                val status = currentUserStory[currentUserStory.lastIndex].status
                for (picture in status) {
                    Picasso.get().load(Uri.parse(picture.imageUrl)).fit().centerCrop()
                        .into(binding.userProfile)
                    binding.userStatusCircles.setPortionsCount(currentUserStory.size)

                }
            } else {
                Picasso.get().load(Uri.parse(userManager.getUserProfileImage())).fit().centerCrop()
                    .into(binding.userProfile)
                Snackbar.make(requireView(), "No More Status", Snackbar.LENGTH_SHORT).show()
            }

            binding.deleteStoriesCardview.visibility = View.GONE
        }

        binding.deleteAllStoriesButton.setOnClickListener {
            viewModel.deleteAllStories()
            currentUserStory.clear()
            Picasso.get().load(Uri.parse(userManager.getUserProfileImage())).fit().centerCrop()
                .into(binding.userProfile)
            Snackbar.make(requireView(), "All status has been deleted", Snackbar.LENGTH_SHORT)
                .show()
            binding.deleteStoriesCardview.visibility = View.GONE
            binding.userStatusCircles.setPortionsCount(0)
        }

        return binding.root
    }

    private fun uploadStatusToFirebase(coverImage: Uri?) {
        val fileName = UUID.randomUUID().toString()//+".jpg"
        viewModel.addStoryImageToFirebaseStorage(coverImage!!, fileName, "Status")

        viewModel.storyImageUrl.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { statusImageFromStorage ->

                if (!statusImageFromStorage.isNullOrBlank() && statusImageFromStorage != previousStory) {
                    val time = Calendar.getInstance().time.time
                    val timestamp = time.toLong()

                    val status = StatusImages(statusImageFromStorage, timestamp)
                    val statusList: ArrayList<StatusImages> = ArrayList()
                    //val userStatusData = UserStatus(userManager.getUserName().toString(),userManager.getUserProfileImage().toString(),timestamp,statusList)


                    val data = kotlin.collections.HashMap<String, Any>()
                    data.put("uploaderUid", auth.currentUser?.uid.toString())
                    data.put("name", userManager.getUserName().toString())
                    data.put("profileImage", userManager.getUserProfileImage().toString())
                    data.put("lastUpdated", status.timeStamp!!)

                    // SENDING STORY TO SERVER
                    viewModel.sendUserStory(data, status)
                    //viewModel.uploadUserStory(data)

                    viewModel.storyUploadStatus.observe(viewLifecycleOwner,
                        androidx.lifecycle.Observer {
                            if (!it.isNullOrBlank()) {
                                if (it == "success") {
                                    mProgressBar.visibility = View.INVISIBLE
                                    Snackbar.make(requireView(),
                                        "Story uploaded successfully",
                                        Snackbar.LENGTH_SHORT).show()
                                    val statusUpdateTime = getTime(timestamp)
                                    binding.statusLastUpdateTime.text = statusUpdateTime
                                    binding.userProfile.borderWidth = 3
                                    binding.userProfile.borderColor = ResourcesCompat.getColor(
                                        requireContext().resources,
                                        R.color.purple_500,
                                        null)
                                } else if (it == "failed") {
                                    mProgressBar.visibility = View.INVISIBLE
                                    Toast.makeText(requireContext(),
                                        "Something went wrong! Please try again",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        })

                    previousStory = statusImageFromStorage

                }

            })
    }

    private fun getTime(lastUpdated: Long): String {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone(cal.timeZone.toZoneId())
        cal.timeInMillis = lastUpdated!!

        var att = ""
        val tt = cal.get(Calendar.AM_PM)
        att = if (tt == 0) {
            "AM"
        } else {
            "PM"
        }
        if (Calendar.getInstance()
                .get(Calendar.HOUR_OF_DAY) - cal.get(Calendar.HOUR_OF_DAY) >= 12
        ) {
            return String.format("%02d:%02d %s on %s",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                att,
                cal.get(Calendar.DATE))
        } else {
            return String.format("today at %02d:%02d %s",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                att)
        }


    }

    private fun showStories(
        storyArray: java.util.ArrayList<MyStory>,
        context: Context,
        name: String,
        userProfile: String,
        time: String,
    ) {
        StoryView.Builder(activity?.supportFragmentManager)
            .setStoriesList(storyArray) // Required
            .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
            .setTitleText(name) // Default is Hidden
            .setSubtitleText("") // Default is Hidden
            .setTitleLogoUrl(Uri.parse(userProfile).toString()) // Default is Hidden
            .setStoryClickListeners(object : StoryClickListeners {
                override fun onDescriptionClickListener(position: Int) {
                    //your action
                }

                override fun onTitleIconClickListener(position: Int) {
                    // navigate current user settings Fragment
                }
            }) // Optional Listeners
            .build() // Must be called before calling show method
            .show()

    }
}