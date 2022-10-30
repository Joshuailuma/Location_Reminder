package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>) : Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource}
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        val repository: ReminderDataSource by inject()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    // When there is an error in the title
    @Test
    fun saveReminderTest_showSnackBarTitleError(): Unit = runBlocking {
        Log.i("KOIN", "Starting saveReminderTest_showSnackBarTitleError")
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        val message = appContext.getString(R.string.err_enter_title)
        onView(withText(message)).check(matches(isDisplayed()))

        activityScenario.close()

        Log.i("KOIN", "Done with saveReminderTest_showSnackBarTitleError")
    }

    @Test
    fun saveReminderTest_showSnackBarLocationError(): Unit = runBlocking{

        Log.i("KOIN", "Starting saveReminderTest_showSnackBarLocationError")

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("A title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("A Description")).perform(
            closeSoftKeyboard())

        onView(withId(R.id.saveReminder)).perform(click())

        val message = appContext.getString(R.string.err_select_location)
        onView(withText(message)).check(matches(isDisplayed()))

        activityScenario.close()
        Log.i("KOIN", "Done with saveReminderTest_showSnackBarLocationError")
    }

    // Show toast when a reminder is saved
    @Test

    fun saveReminderShowToast(): Unit = runBlocking{
        // Espresso Toast test doesn't work on Android 11 and above. Please test on Android 10 and below

        if (android.os.Build.VERSION.SDK_INT <= 29){

            Log.i("KOIN", "Starting saveReminderShowToast")
            // Start up Reminders activity screen.
            val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
            //  after you launch the activity scenario, you use monitorActivity to associate the activity with the dataBindingIdlingResource
            dataBindingIdlingResource.monitorActivity(activityScenario)
            val activity = getActivity(activityScenario)

            onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
            onView(withId(R.id.addReminderFAB)).perform(click())
            onView(withId(R.id.reminderTitle)).perform(replaceText("A Reminder"))
            onView(withId(R.id.reminderDescription)).perform(replaceText("The Description"))
            onView(withId(R.id.selectLocation)).perform(click())
            // My network is pretty slow
            Thread.sleep(8000)
            onView(withId(R.id.map)).perform(longClick())
            onView(withId(R.id.saveReminder)).perform(click())
            onView(withText(R.string.reminder_saved))
                .inRoot(RootMatchers.withDecorView(not(
                    `is`(
                        activity!!.window.decorView
                    )
                ))).check(
                    matches(isDisplayed())
                )

            activityScenario.close()
            Log.i("KOIN", "Done with saveReminderShowToast")
        }
        else{
            Log.i("KOIN", "Cant Test for Toast because of your android version")
        }
        }
}


