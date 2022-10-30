package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource( var reminders: MutableList<ReminderDTO>? = mutableListOf()): ReminderDataSource {
    private var shouldReturnError = false
    fun setReturnError(shouldReturn: Boolean){
        this.shouldReturnError = shouldReturn
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) return Result.Error("ERROR")
        else {
            // Return the reminders
            reminders?.let { return Result.Success(ArrayList(it)) }
            return Result.Success(ArrayList())
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) return Result.Error("ERROR")
        else{
            reminders?.firstOrNull { it.id == id }?.let { return Result.Success(it) }
            return Result.Success(ReminderDTO("", "","",0.0,0.0, ""))
        }
            }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

}