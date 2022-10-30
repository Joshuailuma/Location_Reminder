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

    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        stopKoin()

        datasource = FakeDataSource()

        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), datasource)
    }

    private fun getReminder(): ReminderDTO {
        return ReminderDTO(
            title = "A Reminder",
            description = "The Description",
            location = "Benin",
            latitude = 7.8,
            longitude = 4.8)
    }

    @Test
    fun check_loading()  = runTest{
        datasource.deleteAllReminders()
        val reminder = getReminder()
        datasource.saveReminder(reminder)

        // Main dispatcher will not run coroutines eagerly for this test
        Dispatchers.setMain(StandardTestDispatcher())

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
        // Data must still be present
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(),
            `is`(false)
        )
    }

    @Test
    fun loadRemindersEmptyList() = runBlockingTest {
        datasource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun shouldReturnError() = runBlockingTest{
        datasource.setReturnError(true)
        remindersListViewModel.loadReminders()
        val value = remindersListViewModel.showSnackBar.getOrAwaitValue()
        assertThat(value, `is`("Reminders not found"))
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}