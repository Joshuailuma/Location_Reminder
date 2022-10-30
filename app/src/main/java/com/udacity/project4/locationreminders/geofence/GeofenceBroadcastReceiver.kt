package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings.System.getString
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.enqueueWork
import com.udacity.project4.utils.sendNotification

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("GeofenceBroadcast", "On receive called")



//:implement the onReceive method to receive the geofencing events at the background
        val geofencingEvent = GeofencingEvent.fromIntent(intent!!)

        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e("fenceError", errorMessage)
            return
        }
//        // Get the transition type.
//        val geofenceTransition = geofencingEvent!!.geofenceTransition
//
//        // Test that the reported transition was of interest.
//        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
//
//            // Get the geofences that were triggered. A single event can trigger
//            // multiple geofences.
//            val triggeringGeofences = geofencingEvent.triggeringGeofences
//
//            // Get the transition details as a String.
//            val geofenceTransitionDetails = getGeofenceTransitionDetails(
//                this,
//                geofenceTransition,
//                triggeringGeofences
//            )
//
//            // Send notification and log the transition details.
////            sendNotification(geofenceTransitionDetails)
//            Log.i(TAG, geofenceTransitionDetails)
//        } else {
//            // Log the error.
//            Log.e(TAG, "Occured")
//        }



        if (geofencingEvent != null) {
            when (geofencingEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.i("GeofenceBroadcast", "No Error in onReceive")
                    enqueueWork(context!!, intent)

                }
            }
        } else{
          val error = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent?.errorCode ?: 3)
            Log.i("GeofenceBroad", error)
           val errorCode = geofencingEvent?.errorCode
            Log.i("GeofenceBroadi", errorCode.toString())

        }

    }
}