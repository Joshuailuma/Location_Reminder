package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {
    private val remindersLocalRepository by inject<ReminderDataSource>()

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob


    companion object {
        private const val JOB_ID = 573

        //  TODO: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }
    override fun onHandleWork(intent: Intent) {
        //handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        val geofenceList: List<Geofence> =
            geofencingEvent?.triggeringGeofences as List<Geofence>
        sendNotification(geofenceList)
    }



    //DONE: get the request id of the current geofence
    private fun sendNotification(Geofences: List<Geofence>) {
        for (i in Geofences) {
            val requestId = when {
                Geofences.isNotEmpty() -> {
                    Log.d(TAG, "sendNotification: " + i.requestId)
                    i.requestId
                }
                else -> {
                    Log.e(TAG, "No Geofence Trigger Found !")
                    return
                }
            }
            Log.i(TAG, "$requestId requestId launched")

            if (requestId.isEmpty()) return

            // referesh repo
//        Log.i(TAG, "$repo The reopsitory launched")


//        Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                val result = remindersLocalRepository.getReminder(requestId)
                Log.i(TAG, remindersLocalRepository.getReminders().toString())


                Log.i(TAG, "Coroutine launched")

                //get the reminder with the request id

                Log.i(TAG, "$result Got reminder from remindersLocalRepository")


                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data

                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                } else {
                    Log.i(TAG, "Reminder details was unsuccessful")


                }
            }
        }
    }
}
