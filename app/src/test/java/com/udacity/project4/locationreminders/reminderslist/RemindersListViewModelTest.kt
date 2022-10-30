package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var datasource: FakeDataSource

    //TODO: provide testing to the RemindersListViewModel and its live data objects
    private lateinit var remindersListViewModel: RemindersListViewModel

    val reminderData = ReminderDTO(
        "A Reminder",
        "The Description",
        "Benin",
        7.8,
        4.8
    )

    @Before
    fun setupViewModel() {

        val mutableList = mutableListOf(reminderData)

        datasource = FakeDataSource(mutableList)


        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), datasource)
    }


    @Test
    fun check_loading()  = runTest{
        // Main dispatcher will not run coroutines eagerly for this test
        Dispatchers.setMain(StandardTestDispatcher())

        // When loading a new reminder
        val mutableList = mutableListOf(reminderData)

        datasource = FakeDataSource(mutableList)

        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), datasource)

        remindersListViewModel.loadReminders()

        // Get the LiveData value for newTaskEvent using getOrAwaitValue
        val value = remindersListViewModel.showLoading.getOrAwaitValue()

        // The progress indicator is shown.
        assertThat(
            value, `is`(true)
        )

        // Wait until complete
        advanceUntilIdle()

        // The progress indicator is not shown.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(),
             `is`(false)
        )
    }

    @Test
    fun shouldReturnError() {
        datasource = FakeDataSource(null)
        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), datasource)

        datasource.setReturnError(true)
        remindersListViewModel.loadReminders()
//        val value = remindersListViewModel.showSnackBar.getOrAwaitValue()
        val value = remindersListViewModel.showSnackBar.getOrAwaitValue()


        assertThat(
            value, `is`("ERROR")
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

}