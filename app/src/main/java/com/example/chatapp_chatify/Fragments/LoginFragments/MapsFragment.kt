package com.example.chatapp_chatify.Fragments.LoginFragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.example.chatapp_chatify.Adapters.MapsPlacesAdapter
import com.example.chatapp_chatify.DataClass.MapsModel.PlacesMarkListModel
import com.example.chatapp_chatify.DataClass.MapsModel.Result
import com.example.chatapp_chatify.DataClass.Users
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.ViewModel.MapsViewModel
import com.example.chatapp_chatify.databinding.FragmentMapsBinding
import com.example.chatapp_chatify.utils.OnItemSelectedListener
import com.example.chatapp_chatify.utils.OnLocationSelectedListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MapsFragment : Fragment() {

    private lateinit var mMap: GoogleMap
    private lateinit var binding : FragmentMapsBinding
    private var selectedPlace : String? =null
    private var selectedRadius : String? =null
    private var previousList : PlacesMarkListModel? = null
    private var listener: OnLocationSelectedListener ? = null

    private var latLng : LatLng? = null


    private val REQUEST_LOCATION_PERMISSION = 33
    var isRecordPermissionGranted = false

    private var currentlySelectedItem : Result ? = null

    private var currentUser : Users? = null

    private lateinit var adapter : MapsPlacesAdapter

    private var url : String? = null

    val viewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMapsBinding.inflate(layoutInflater)
        val actionbar : android.app.ActionBar? = requireActivity().actionBar
        actionbar?.setDisplayShowHomeEnabled(true)
        actionbar?.setDisplayUseLogoEnabled(true)

        setupSpinner()

        if(requireArguments().getParcelable<Users>("UserData")!=null)
        {
            currentUser = requireArguments().getParcelable<Users>("UserData")!!
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment

        mapFragment.getMapAsync{googleMap->
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
                mMap = googleMap
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                mMap.uiSettings.isZoomControlsEnabled = true
                val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val criteria = Criteria()
                criteria.accuracy = Criteria.ACCURACY_COARSE
                val provider = locationManager.getBestProvider(criteria, true)
                val location = locationManager.getLastKnownLocation(provider!!)
                if (location != null) {
                    latLng = LatLng(location.latitude, location.longitude)
                    Log.d("currentLocation",latLng.toString())
                    val markerOptions = MarkerOptions()
                    mMap.clear()
                    markerOptions.title("Your Location")
                    markerOptions.position(latLng!!)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng!!, 17f))
                }

            } else {
                // Request the location permission
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            }

        }




        adapter = MapsPlacesAdapter(requireContext())
        binding.placeList.adapter = adapter

        adapter.setOnItemSelectedListener(object :OnItemSelectedListener{
            override fun onItemSelected(item: Result) {
                val markerOptions = MarkerOptions()
                listener?.onLocationSelected(item)
                currentlySelectedItem = item
                val latLng = LatLng(item.geometry.location.lat,item.geometry.location.lng)
                markerOptions.position(latLng)
                markerOptions.title(item.name)
                markerOptions.snippet("${item.formatted_address}  Rating : ${item.rating}★")
                mMap.clear()
                mMap.addMarker(markerOptions)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17f))

            }
        })


        binding.searchLocations.setOnClickListener {
            binding.placeTypeSpinner.setSelection(0)
            binding.radiusSpinner.setSelection(0)
            url = getQueryURL()
            viewModel.getMapsLocation(url!!)
        }
        viewModel.data.observe(viewLifecycleOwner, Observer { data->
            if(data.results.isNotEmpty() && data.results!=previousList?.results)
            {
                // PARSE THE LIST TO ADAPTER
                adapter.submitList(data.results)
                previousList = data
                Log.d("search Result", data.results[0].name)
            }
        })

        binding.switchButton.setOnClickListener {
            if (binding.placeList.visibility == View.VISIBLE) {
                binding.placeList.visibility = View.GONE

            } else {
                binding.placeList.visibility = View.VISIBLE
            }

        }

        binding.sendButton.setOnClickListener {
            if(currentlySelectedItem!=null)
            {
                Log.d("data",currentlySelectedItem!!.name)
                val bundle = Bundle()
                bundle.putParcelable("Location",currentlySelectedItem)
                bundle.putParcelable("UserData",currentUser)
                Navigation.findNavController(requireView()).navigate(R.id.action_mapsFragment_to_userChatFragment,bundle)
            }
            else{
                Snackbar.make(requireView(),"Please Select Any Location",Snackbar.LENGTH_SHORT).show()
            }

        }

        return binding.root
    }

    private fun setupSpinner() {

       binding.placeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

           override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long )
           {
               selectedPlace = resources.getStringArray(R.array.place_types)[position]
               url = getQueryURL()
               viewModel.getMapsLocation(url!!)

           }

           override fun onNothingSelected(parent: AdapterView<*>?) {
               selectedPlace = resources.getStringArray(R.array.place_types)[0]
           }

       }

        binding.radiusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRadius = resources.getStringArray(R.array.radius_array_int)[position]

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRadius = resources.getStringArray(R.array.radius_array_int)[1]
            }

        }

    }

    override fun onPause() {
        //locationSelectedListener?.onLocationSelected(currentlySelectedItem!!)
        listener?.onLocationSelected(currentlySelectedItem!!)
        super.onPause()
    }

    override fun onDestroy() {
       listener?.onLocationSelected(currentlySelectedItem!!)
        super.onDestroy()
    }

    private fun getQueryURL() : String {
        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/textsearch/json?")
        googlePlaceUrl.append("query=${selectedPlace!!.lowercase(Locale.ROOT)}")
        googlePlaceUrl.append("&location=$latLng")
        googlePlaceUrl.append("&radius=$selectedRadius")
        googlePlaceUrl.append("&key=AIzaSyDZ8ciIxalJd0DsCQZWtuaWjgrtiQzk66I")
        return googlePlaceUrl.toString()

    }

    fun setOnLocationSelectedListener(listener: OnLocationSelectedListener) {
        this.listener = listener
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_LOCATION_PERMISSION){
            isRecordPermissionGranted = grantResults[0] ==PackageManager.PERMISSION_GRANTED
        }
    }
}