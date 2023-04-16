package com.example.chatapp_chatify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.chatapp_chatify.CallingActivities.AudioConferenceActivity
import com.example.chatapp_chatify.CallingActivities.VideoConferenceActivity
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.Fragments.UserChatFragment
import com.example.chatapp_chatify.ViewModel.FirebaseMessagesViewModel
import com.example.chatapp_chatify.databinding.ActivityMainBinding
import com.example.chatapp_chatify.utils.Constant
import com.example.chatapp_chatify.utils.Constant.Companion.TAG
import com.example.chatapp_chatify.utils.UserManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var userName: String
    private lateinit var userNumber: String
    private lateinit var userImage: String

    @Inject
    lateinit var userManager: UserManager

    var currentReceiver: Users? = null

    private val messageViewModel by viewModels<FirebaseMessagesViewModel>()


    // ASKING FOR PERMISSIONS
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private var isReadPermissionGranted = false
    private var isLocationPermissionGranted = false
    private var isCameraPermissionGranted = false
    private var isRecordPermissionGranted = false
    private var isNotificationPermissionGranted = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

                isReadPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: isReadPermissionGranted
                isLocationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                    ?: isLocationPermissionGranted
                isCameraPermissionGranted =
                    permissions[Manifest.permission.CAMERA] ?: isCameraPermissionGranted
                isRecordPermissionGranted =
                    permissions[Manifest.permission.RECORD_AUDIO] ?: isRecordPermissionGranted
                isNotificationPermissionGranted = permissions[if (Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    Log.d(Constant.TAG, Build.VERSION.SDK_INT.toString())
                }] ?: isNotificationPermissionGranted
            }
        requestPermission()

        val appbar = binding.appBar

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val navController = navHostFragment!!.findNavController()

        setSupportActionBar(appbar)

        appbar.title = ""

        val popupMenu = PopupMenu(this, null)
        popupMenu.inflate(R.menu.bottom_navigation)
        binding.bottomBar.setupWithNavController(popupMenu.menu, navController)


        toggle = getActionBarDrawerToggle(binding.mainDrawer, binding.appBar)
        binding.sideDrawer.setNavigationItemSelectedListener(this)





        binding.bottomBar.onItemSelected = {
            when (it) {
                0 -> {

                }
                1 -> {
                    onBackPressed()
                }
                2 -> {

                }
                3 -> {

                }
            }

        }

        //SETTING UP SIDE NAVIGATION HEADER PROFILE
        val header = binding.sideDrawer.getHeaderView(0)
        val headerTitle = header.findViewById<TextView>(R.id.header_user_name)
        val headerImage = header.findViewById<ImageView>(R.id.header_user_image)
        val headerNumber = header.findViewById<TextView>(R.id.header_user_number)

        navController.addOnDestinationChangedListener(object :
            NavController.OnDestinationChangedListener {

            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?,
            ) {

                when (destination.id) {
                    R.id.welcomePage -> {
                        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        binding.appBarLayout.visibility = View.GONE
                        binding.bottomBar.visibility = View.GONE
                        binding.sideDrawer.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = false

                    }
                    R.id.authenticationFragment -> {
                        binding.appBarLayout.visibility = View.GONE
                        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        binding.bottomBar.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = false
                        binding.sideDrawer.visibility = View.GONE
                    }
                    R.id.enterOTPFragment -> {
                        binding.appBarLayout.visibility = View.GONE
                        binding.bottomBar.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = false
                        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        binding.sideDrawer.visibility = View.GONE
                    }
                    R.id.registerationFragment -> {
                        binding.appBarLayout.visibility = View.GONE
                        binding.bottomBar.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = false
                        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        binding.sideDrawer.visibility = View.GONE
                    }
                    R.id.chatScreenFragment -> {

                        binding.appBarLayout.visibility = View.VISIBLE
                        binding.bottomBar.visibility = View.VISIBLE
                        binding.appBar.visibility = View.VISIBLE
                        toggle.isDrawerIndicatorEnabled = true
                        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                        binding.sideDrawer.visibility = View.VISIBLE
                        binding.userDetails.visibility = View.GONE
                        binding.bottomBar.visibility = View.VISIBLE
                        appbar.logo = null

                        userName = userManager.getUserName().toString()
                        userNumber = userManager.getUserNumber().toString()
                        userImage = userManager.getUserProfileImage().toString()
                        binding.appBar.setTitle(R.string.app_name)
                        headerTitle.text = userName
                        headerNumber.text = userNumber
                        if (userImage != Constant.USER_IMAGE_FILE) {
                            Glide.with(this@MainActivity).load(userImage)
                                .placeholder(this@MainActivity.getDrawable(R.drawable.user))
                                .centerCrop().into(headerImage)
                        }

                    }

                    R.id.callsFragment -> {
                        binding.userDetails.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = true
                        binding.bottomBar.visibility = View.VISIBLE
                        appbar.logo = null

                    }
                    R.id.settingsFragment -> {
                        binding.userDetails.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = true
                        binding.bottomBar.visibility = View.VISIBLE
                        appbar.logo = null
                    }
                    R.id.webFragment2 -> {
                        binding.userDetails.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = true
                        binding.bottomBar.visibility = View.VISIBLE
                        appbar.logo = null
                    }
                    R.id.userChatFragment -> {
                        binding.userDetails.visibility = View.VISIBLE
                        binding.userName.text = userManager.getOppositeUserName()
                        binding.appBar.setTitle(R.string.space)
                        binding.mainDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        Glide.with(this@MainActivity)
                            .load(userManager.getOppositeUserProfileImage()).centerCrop()
                            .into(binding.userImage)
                        binding.bottomBar.visibility = View.GONE
                        toggle.isDrawerIndicatorEnabled = false
                        binding.appBar.setNavigationIcon(R.drawable.back_button)


                    }

                }


            }

        })



        toggle.setToolbarNavigationClickListener {
            binding.sideDrawer.menu[0].isChecked = true
            navController.navigateUp()

        }


    }


    private fun getActionBarDrawerToggle(
        mainDrawer: DrawerLayout,
        appBar: MaterialToolbar,
    ): ActionBarDrawerToggle {
        val toggle = ActionBarDrawerToggle(this, mainDrawer, appBar, R.string.open, R.string.close)
        mainDrawer.addDrawerListener(toggle)
        toggle.syncState()
        return toggle
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    //NEEDS TO BE EDITED FOR SIDE NAVIGATION DRAWER
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("Drawer Navigation", "Drawer Navigation clicked")
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val navController = navHostFragment!!.findNavController()
        when (item.itemId) {
            R.id.myAccount_drawer -> {
                navController.navigate(R.id.settingsFragment)
            }
            R.id.calls_drawer -> {
                navController.navigate(R.id.callsFragment)
            }
            R.id.web_drawer -> {
                navController.navigate(R.id.webFragment2)
            }
            R.id.signOut_drawer -> {
                Firebase.auth.signOut()
                navController.navigate(R.id.welcomePage, null,
                    NavOptions.Builder().setPopUpTo(R.id.chatScreenFragment, true).build())
            }
            else -> {
                Log.d("Invalid", "Invalid selection")
            }

        }

        binding.mainDrawer.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        val navController = navHostFragment!!.findNavController()

        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        when (item.itemId) {

            R.id.videoCall_appBar -> {
                Toast.makeText(this, "video call started", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, VideoConferenceActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelable("User", currentReceiver)
                bundle.putInt("callType", Constant.CALL_TYPE_VIDEO)
                if (!Constant.CALL_TOKEN.isNullOrBlank()) {
                    bundle.putString("Token", Constant.CALL_TOKEN)
                }
                intent.putExtras(bundle)
                startActivity(intent)
                //navController.navigate(R.id.action_userChatFragment_to_conferenceFragment,bundle)
            }
            R.id.audioCall_appBar -> {
                Toast.makeText(this, "audio call started", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AudioConferenceActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelable("User", currentReceiver)
                bundle.putInt("callType", Constant.CALL_TYPE_AUDIO)
//                if(!Constant.CALL_TOKEN.isNullOrBlank())
//                {
//                    bundle.putString("Token",Constant.CALL_TOKEN)
//                }
                intent.putExtras(bundle)
                startActivity(intent)
                //navController.navigate(R.id.action_userChatFragment_to_conferenceFragment,bundle)
            }
            else -> {
                Toast.makeText(applicationContext, "Something Went Wrong!!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (binding.mainDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.mainDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }

    }

    private fun requestPermission() {
        isReadPermissionGranted = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        isCameraPermissionGranted = ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        isLocationPermissionGranted = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        isRecordPermissionGranted = ContextCompat.checkSelfPermission(this,
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED


        val pendingPermissionRequest: MutableList<String> = ArrayList()

        if (!isReadPermissionGranted) {
            pendingPermissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            val snack = Snackbar.make(binding.root,
                "Please grant required Read permission",
                Snackbar.LENGTH_SHORT)
            snack.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
            snack.show()

        }
        if (!isLocationPermissionGranted) {
            pendingPermissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            val snack = Snackbar.make(binding.root,
                "Please grant required Location permission",
                Snackbar.LENGTH_SHORT)
            snack.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
            snack.show()
        }
        if (!isCameraPermissionGranted) {
            pendingPermissionRequest.add(Manifest.permission.CAMERA)
            val snack = Snackbar.make(binding.root,
                "Please grant required Camera permission",
                Snackbar.LENGTH_SHORT)
            snack.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
            snack.show()
        }

        if (!isRecordPermissionGranted) {
            pendingPermissionRequest.add(Manifest.permission.RECORD_AUDIO)
            val snack = Snackbar.make(binding.root,
                "Please grant required Record Audio permission",
                Snackbar.LENGTH_SHORT)
            snack.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
            snack.show()
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (!isNotificationPermissionGranted) {
                pendingPermissionRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                val snack = Snackbar.make(binding.root,
                    "Please grant required Notification Permission",
                    Snackbar.LENGTH_SHORT)
                snack.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                snack.show()
            }
        }

        if (pendingPermissionRequest.isNotEmpty()) {
            permissionLauncher.launch(pendingPermissionRequest.toTypedArray())
        }

    }

    fun setCurrentReceiverUser(user: Users) {
        currentReceiver = user
    }

}
