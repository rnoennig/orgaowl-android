package de.rnoennig.orgaowl.model

import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.persistence.TasklistWithTasks
import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getAllTasks(): Flow<List<Task>>

    fun getAllTasklists(): List<Tasklist>

    fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>>

    suspend fun insertTask(task: Task)

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)

    suspend fun insertTasklist(tasklist: Tasklist)

    suspend fun updateTasklist(tasklist: Tasklist)

    suspend fun deleteTasklist(tasklist: Tasklist)
}