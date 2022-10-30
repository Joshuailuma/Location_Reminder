package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

@get:Rule
var instantExecutorRule = InstantTaskExecutorRule()
    // , create a lateinit field for your database:
    private lateinit var database: RemindersDatabase
    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    val reminderData = ReminderDTO(
        "A Reminder",
        "The Description",
        "Benin",
        7.8,
        4.8
    )


    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - Insert a reminder.
        database.reminderDao().saveReminder(reminderData)

        // WHEN - Get the task by id from the database.
        val loaded = database.reminderDao().getReminderById(reminderData.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminderData.id))
        assertThat(loaded.title, `is`(reminderData.title))
        assertThat(loaded.location, `is`(reminderData.location))
        assertThat(loaded.latitude, `is`(reminderData.latitude))
        assertThat(loaded.longitude, `is`(reminderData.longitude))
    }

    @Test
    fun noDataFoundWithId() = runBlockingTest {
        val reminder = database.reminderDao().getReminderById("923")

        assertThat(reminder, nullValue())

    }


    // Make an @After method for cleaning up your database using database.close():
    @After
    fun closeDb() = database.close()


}