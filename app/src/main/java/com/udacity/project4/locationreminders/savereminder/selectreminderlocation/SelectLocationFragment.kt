package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority.PRIORITY_LOW_POWER
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


private const val REQUEST_CODE_BACKGROUND = 102929
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 12433
private const val REQUEST_LOCATION_PERMISSION = 1


class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var  poi: PointOfInterest
    private lateinit var map: GoogleMap
    private var isLocationSelected = false

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
        val latitude = 6.342283
        val longitude = 5.632208
        val homeLatLng = LatLng(latitude, longitude)
      // Enable location
      requestLocation()
        // Move the camera and zoom
        val zoomLevel = 15f
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

      setMapLongClick(map)
        // Set point of interest
        setPoiClick(map)
        // Add style
        setMapStyle(map)
        // Get user location
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


    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                checkDeviceLocationSettings()
            } else {
                requestPermissionForQ()
            }

        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        // Zoom to location
        map.moveCamera(CameraUpdateFactory.zoomIn())
    }

    // Check if permission is granted
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestPermissionForQ() {
        val hasForegroundPermission = ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasForegroundPermission) {
            val hasBackgroundPermission = ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasBackgroundPermission) {
                checkDeviceLocationSettings()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_CODE_BACKGROUND
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BACKGROUND) {
            checkDeviceLocationSettings()
        }
    }

    // Check if user turned on location, if not, ask him to
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = PRIORITY_LOW_POWER
        }
        val requestBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(requestBuilder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Could not get location")
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
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
