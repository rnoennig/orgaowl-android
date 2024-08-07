package de.rnoennig.orgaowl.model

import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.TaskDao
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.persistence.TasklistDao
import de.rnoennig.orgaowl.persistence.TasklistWithTasks
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val tasklistDao: TasklistDao
) : ITaskRepository {
    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    override fun getAllTasklists(): List<Tasklist> = tasklistDao.getAllTasklists()

    override fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>> = tasklistDao.getAllTasklistsWithTasks()

    override suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    override suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    override suspend fun insertTasklist(tasklist: Tasklist) = tasklistDao.insertTasklist(tasklist)
    override suspend fun updateTasklist(tasklist: Tasklist) = tasklistDao.updateTasklist(tasklist)
    override suspend fun deleteTasklist(tasklist: Tasklist) = tasklistDao.deleteTasklist(tasklist)
}