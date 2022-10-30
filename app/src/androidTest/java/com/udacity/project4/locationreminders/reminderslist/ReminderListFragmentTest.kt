package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {
    private lateinit var datasource: FakeDataSource
    private lateinit var reminderListViewModel: RemindersListViewModel

    @Before
    fun init() {
        // Get data source
        datasource = FakeDataSource()

        stopKoin()
        reminderListViewModel =
            RemindersListViewModel(getApplicationContext(), datasource)

        val myModule = module {
            single {
                reminderListViewModel
            }
        }
        // Koin module
        startKoin {
            modules(listOf(myModule))
        }
    }
//  : test the navigation of the fragments.

    @Test
    fun navigateToAddReminder() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.addReminderFAB))
            .perform(click())
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }


    // : test the displayed data on the UI.
    @Test
    fun checkIfReminderIsDisplayed() = runBlockingTest {
        val reminderData = ReminderDTO(
            "A Reminder",
            "The Description",
            "Benin",
            7.8,
            4.8
        )

        // Save the reminder to db

        datasource.saveReminder(reminderData)
        // Check if title description and location are displayed
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText(reminderData.title)).check(matches(isDisplayed()))
        onView(withText(reminderData.description)).check(matches(isDisplayed()))
        onView(withText(reminderData.location)).check(matches(isDisplayed()))
    }

    //    TODO: add testing for the error messages.
    @Test
    fun noData() = runBlockingTest {
        datasource.deleteAllReminders()

        launchFragmentInContainer<ReminderListFragment>(Bundle.EMPTY, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }


}
