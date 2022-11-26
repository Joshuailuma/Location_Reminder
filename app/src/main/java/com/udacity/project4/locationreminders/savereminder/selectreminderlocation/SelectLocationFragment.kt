package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SelectLocationFragment : BaseFragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var  poi: PointOfInterest
    private lateinit var map: GoogleMap
    var latitude = 6.342283
    var longitude = 5.632208
    private var isLocationSelected = false

    private lateinit var cameraPosition: CameraPosition


    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 30
        fastestInterval = 10
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        maxWaitTime = 60
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//     add the map setup implementation
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(mapCallback)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this.requireActivity())

        locationSelected()
        return binding.root
    }

    private fun locationSelected(){

        binding.saveButton.setOnClickListener{
            if (isLocationSelected){
                Log.i("Location", "${poi.name}, ${poi.latLng}, ${poi.placeId}")
                onLocationSelected(poi)
            } else{
                Toast.makeText(context, getString(R.string.select_location), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun onLocationSelected(poi: PointOfInterest) {
        //        When the user confirms on the selected location,
        //         send back the selected location details to the view model

        val latLng = poi.latLng
        _viewModel.reminderSelectedLocationStr.value = poi.name
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        //  and navigate back to the previous fragment to save the reminder and add the geofence
        findNavController().popBackStack()
    }

  private val mapCallback =  OnMapReadyCallback{ googleMap->
        map = googleMap

      if (isPermissionGranted()) {
          Log.i("Location", "Permission granted before, getting logitude")
          getLastKnownLocation()
      } else {
          Log.i("Location", "About to request permission")

          checkLocationPermission()
      }
      setMapLongClick(map)
        // Set point of interest
        setPoiClick(map)
        // Add style
        setMapStyle(map)
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener {
            map.clear()
            binding.saveButton.setOnClickListener { view ->
                _viewModel.latitude.value = it.latitude
                _viewModel.longitude.value = it.longitude
                _viewModel.reminderSelectedLocationStr.value = "Custom location"
            }

            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(it, 15f)
            map.moveCamera(cameraUpdate)
            val poiMarker = map.addMarker(MarkerOptions().position(it))
            poiMarker?.showInfoWindow()
            poi = PointOfInterest(it, "Custom", "Location")
            onLocationSelected(poi)
            isLocationSelected = true
        }

    }

    // Point of interest
    private fun setPoiClick(map: GoogleMap) {
        // Set the click listener
        map.setOnPoiClickListener { poii ->
            map.clear()
            // Add a marker
            // To make it show the info window
            map.addMarker(
                MarkerOptions()
                    .position(poii.latLng)
                    .title(poii.name)
            // make poi show
            )?.showInfoWindow()
            poi = poii
            onLocationSelected(poi)
            isLocationSelected = true
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                context?.let {
                    MapStyleOptions.loadRawResourceStyle(
                        this.requireContext(),
                        R.raw.map_style
                    )
                }
            )
            // If styling fails
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
            // Couldnlt load file
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProvider?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this.requireContext())
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        }
    }


    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ), MY_PERMISSIONS_REQUEST_LOCATION)
    }


    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                //The last location in the list is the newest
                val location = locationList.last()
                latitude =  location.latitude
                longitude = location.longitude
//                Log.i("Location", "New location from locationCallback is $longitude  $latitude")

                 cameraPosition = CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(15f)
                    .build()
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            }
        }
    }

    private fun getLastKnownLocation() {
        // Getting last location

        Log.i("Location", "Permission granted getting user location")
        map.isMyLocationEnabled = true

        fusedLocationProvider!!.lastLocation
            .addOnSuccessListener { location ->
                // If request is successful and location is not null
                if (location != null) {
                    // use your location object
                    // get latitude , longitude and other info from this
                    longitude = location.longitude
                    latitude = location.latitude
//                    Log.i("Location", "Location from getLastKnownLocation is $longitude $latitude")

                    cameraPosition = CameraPosition.Builder()
                        .target(LatLng(latitude, longitude))
                        .zoom(15f)
                        .build()
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                } else {
                    // "Location is null"
                    // Request for permission
                    checkLocationPermission()
                }
            }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this.requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        getLastKnownLocation()
                    }

                } else {
                    // permission denied! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this.requireContext(), R.string.location_required_error, Toast.LENGTH_LONG).show()

                }
                return
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
