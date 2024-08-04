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
    /*
    @Query("SELECT * FROM Tasklist")
    fun getAllTasklists(): Flow<List<Tasklist>>

    @Transaction
    @Query("SELECT * FROM Tasklist where name = :taskListName")
    fun getTasklistWithTasks(taskListName: String): List<TasklistWithTasks>
*/

    @Transaction
    @Query("SELECT * FROM Tasklist")
    fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>>

    @Insert
    fun insertTasklist(tasklist: Tasklist)

    @Update
    fun updateTasklist(tasklist: Tasklist)

    @Delete
    fun deleteTasklist(tasklist: Tasklist)
}