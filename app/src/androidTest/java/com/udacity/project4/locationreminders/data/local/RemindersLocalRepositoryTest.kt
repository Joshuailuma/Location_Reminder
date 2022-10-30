package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the localRepository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase
    private lateinit var localRepository: RemindersLocalRepository

    // Initialize the db before anything
    @Before
    fun setup() {
         // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localRepository =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    val reminderData = ReminderDTO(
        "A Reminder",
        "The Description",
        "Benin",
        7.8,
        4.8
    )
    @Test
    fun saveTask_retrievesReminder() = runBlocking {

        localRepository.saveReminder(reminderData)
        val result = localRepository.getReminder(reminderData.id)
        result as Result.Success

        assertThat(result.data.id, `is`(reminderData.id))
        assertThat(result.data.description, `is`(reminderData.description))
        assertThat(result.data.location, `is`(reminderData.location))
        assertThat(result.data.latitude, `is`(reminderData.latitude))
        assertThat(result.data.longitude, `is`(reminderData.longitude))
    }

    @Test
    fun deleteReminderTest() = runBlocking {

        localRepository.saveReminder(reminderData)
        localRepository.deleteAllReminders()
        val result = localRepository.getReminders()
        result as Result.Success<List<ReminderDTO>>

        assertThat(result.data.isEmpty(), `is`(true))

    }

    @Test
    fun noRemindersFound_GetReminderById() = runBlocking {
        val result = localRepository.getReminder("936")
        result as Result.Error
        assertThat(result.message, notNullValue())
        assertThat(result.message, `is`("Reminder not found!"))
    }
}