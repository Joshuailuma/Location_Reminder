package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource( var reminders: MutableList<ReminderDTO> = mutableListOf()): ReminderDataSource {
    private var shouldReturnError = false

    fun setReturnError(value: Boolean){
         shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError){
            Result.Error("Reminders were unable to get retrieved")
        }else{
            Result.Success(ArrayList(reminders))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if(shouldReturnError){
            Result.Error("Reminder was unable to get retrieved")
        } else {
            // Check the list to find a reminder with matching id
            val reminder = reminders.find{
                it.id == id
            }
            if (reminder != null)
                Result.Success(reminder)
            else Result.Error("Reminder not found!")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

}