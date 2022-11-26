package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

@RequiresApi(Build.VERSION_CODES.M)
class SaveReminderFragment : BaseFragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private val TAG = "SaveReminderFragment"

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem
    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, FLAG_UPDATE_CURRENT)
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

                // Use the user entered reminder details to:
                //  1) add a geofencing request
                // A GeofencingClient is the main entry point for interacting with the geofencing APIs
                geofencingClient = LocationServices.getGeofencingClient(requireActivity())

                // 2) save the reminder to the local db
                 reminderDataItem = ReminderDataItem(
                    title = title,
                    description = description,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                )

            if (_viewModel.validateEnteredData(reminderDataItem)){
                checkPermissionsAndStartGeofencing()
            }

            }
        }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        // check if the ACCESS_FINE_LOCATION permission is granted
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(this.requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))
        // If the device is running Q or higher, check that the ACCESS_BACKGROUND_LOCATION..
        // permission is granted
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this.requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                // Return true if the permissions are granted and false if not.
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        // create a LocationRequest, a LocationSettingsRequest Builder.
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // use LocationServices to get the Settings Client and create a val called..
        // locationSettingsResponseTask to check the location settings
        val settingsClient = LocationServices.getSettingsClient(this.requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        // Since the case we are most interested in here is finding out if the location settings...
        // are not satisfied, add an onFailureListener() to the locationSettingsResponseTask
        locationSettingsResponseTask.addOnFailureListener { exception ->
            // Check if the exception is of type ResolvableApiException and if so, try calling the startResolutionForResult()..
            // method in order to prompt the user to turn on device location.
            if (exception is ResolvableApiException && resolve){
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resultLauncher.launch(intentSenderRequest)
                } catch (sendException: IntentSender.SendIntentException) {
                    Log.i(TAG, "Error getting location settings" + sendException.message)
                }
                // If the exception is not of type ResolvableApiException, present a snackbar that alerts..
                // the user that location needs to be enabled to save reminder
            } else {
                Snackbar.make(
                    binding.selectLocation,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }

        // locationSettingsResponseTask
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofenceForClue(reminderDataItem)
            }
        }
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        // If the permissions have already been approved, you don’t need to ask again.
        // Return (leave method)
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        // The permissionsArray contains the permissions that are going to be requested. Initially, add..
        // ACCESS_FINE_LOCATION since that will be needed on all API levels.
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        // you will need a resultCode. The code will be different depending on if the device is running Q or ..
        // later and will inform us if you need to check for one permission (fine location) or multiple permissions..
        // (fine and background location) when the user returns from the permission request screen. Add a when statement..
        // to check the version running and assign result code to REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE if..
        // the device is running Q or later and REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE if not
        val resultCode = when {
            runningQOrLater -> {
                Log.i(TAG, "Requesting background and foregroud location permission")
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        // Request permissions passing in the current activity, the permissions..
        // array and the result code
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Log.i("Locations", "Permissions were denied")

            Snackbar.make(
                this.requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            Log.i("Locations", "Permissions were granted")
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }


    private fun addGeofenceForClue(reminder: ReminderDataItem) {
        if (reminder.id.length > 2) {
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(reminder.latitude!!, reminder.longitude!!, 100f)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            Log.d(TAG, "${reminder.id} reminder id triggered ")
            Log.d(TAG, "${geofence.requestId} geofence id triggered ")

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    _viewModel.validateAndSaveReminder(
                        reminderDataItem
                    )
                    Log.d(TAG, "Geofence Added ")
                }
                addOnFailureListener {

                    Log.d(TAG, "failed to create geofence: $it")
                }
            }
        } else {
            Log.d(TAG, "failed to create geofence: COuld not determine reminder id ")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
