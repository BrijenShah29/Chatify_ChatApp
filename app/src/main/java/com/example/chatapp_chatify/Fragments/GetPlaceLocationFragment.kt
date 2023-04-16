package com.example.chatapp_chatify.Fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.example.chatapp_chatify.R
import com.example.chatapp_chatify.databinding.FragmentGetPlaceLocationBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class GetPlaceLocationFragment : Fragment() {


    lateinit var binding: FragmentGetPlaceLocationBinding
    private lateinit var mMap: GoogleMap
    lateinit var location: String
    lateinit var latLng: LatLng
    lateinit var placeTitle: String
    lateinit var placeAddress: String

    private val REQUEST_LOCATION_PERMISSION = 22
    var isRecordPermissionGranted = false

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            isRecordPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGetPlaceLocationBinding.inflate(layoutInflater)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment

        val message = requireArguments().getString("Location")
        val data = message!!.split("&")
        location = data[0]
        placeTitle = data[1]
        placeAddress = data[2]
        val latLngArray = location.split("(", ",", ")")
        Log.d("location", latLngArray.toString())
        val lat = latLngArray[1].toDouble()
        val lng = latLngArray[2].toDouble()
        latLng = LatLng(lat, lng)
        Log.d("location", latLng.toString())

        binding.locationTitle.text = "Place Name: $placeTitle"
        binding.locationAddress.text = "Place Address : $placeAddress"

        mapFragment.getMapAsync { googleMap ->
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
                mMap = googleMap
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                mMap.uiSettings.isZoomControlsEnabled = true
                val locationManager =
                    requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val criteria = Criteria()
                criteria.accuracy = Criteria.ACCURACY_COARSE
                val provider = locationManager.getBestProvider(criteria, true)
                val location = locationManager.getLastKnownLocation(provider!!)
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    Log.d("currentLocation", currentLatLng.toString())
                    val markerOptions = MarkerOptions()
                    mMap.clear()
                    markerOptions.title("Your Location")
                    markerOptions.position(currentLatLng)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                    setupTheMarker()
                }

            } else {
                // Request the location permission
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION)
            }

        }

        return binding.root
    }

    private fun setupTheMarker() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            val markerOptions = MarkerOptions()
            if (latLng != null) {
                markerOptions.position(latLng)
            }
            markerOptions.title(placeTitle)
            markerOptions.snippet(placeAddress)
            //Adding marker to map
            mMap.addMarker(markerOptions)
            //Adding Camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng!!))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
    }


}