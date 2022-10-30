package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    //TODO: provide testing to the SaveReminderView and its live data objects
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var datasource: FakeDataSource

    // Create a reminderData object
    val reminderData = ReminderDataItem(
        "A Reminder",
        "The Description",
        "Benin",
        7.8,
        4.8
    )

    @Test
    fun check_loading() {
        datasource = FakeDataSource()
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), datasource)
        // Pause dispatcher to set loading spinner
        mainCoroutineRule.pauseDispatcher()
        // Save the reminder we just created
        saveReminderViewModel.saveReminder(reminderData)
        // Show the spinner
        val value = saveReminderViewModel.showLoading.getOrAwaitValue()
        // Check if its spinning
        assertThat(value, `is`(true))
    }

    @Test
    fun shouldReturnError() {
        datasource = FakeDataSource(null)
        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), datasource)
        reminderData.title = null
        saveReminderViewModel.validateAndSaveReminder(reminderData)
        val value = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        //Check if the error shows that string value
        assertThat(value, `is`(R.string.err_enter_title))
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}