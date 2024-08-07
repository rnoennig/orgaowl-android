package de.rnoennig.orgaowl.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TasklistDao {

    @Transaction
    @Query("SELECT * FROM Tasklist")
    fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>>

    @Insert
    fun insertTasklist(tasklist: Tasklist)

    @Update
    fun updateTasklist(tasklist: Tasklist)

    @Delete
    fun deleteTasklist(tasklist: Tasklist)

    @Query("SELECT * FROM Tasklist")
    fun getAllTasklists(): List<Tasklist>
}