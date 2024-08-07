package de.rnoennig.orgaowl.model

import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.persistence.TasklistWithTasks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ListBackedTaskRepository(
    private val allTasklists: MutableList<Tasklist> = mutableListOf(),
    private val allTasks: MutableList<Task> = mutableListOf()
) : ITaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return flowOf(allTasks)
    }

    override fun getAllTasklists(): List<Tasklist> {
        return allTasklists
    }

    override fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>> {
        return flowOf(allTasklists.map { tasklist ->
            TasklistWithTasks(
                tasklist = tasklist,
                tasks = allTasks.filter { it.tasklist == tasklist.uuid })
        })
    }

    override suspend fun insertTask(task: Task) {
        allTasks.add(task)
    }

    override suspend fun updateTask(task: Task) {
        val indexOfItem = allTasks.indexOfFirst { it.uuid == task.uuid }
        allTasks[indexOfItem] = task
    }

    override suspend fun deleteTask(task: Task) {
        allTasks.removeIf { it.uuid == task.uuid }
    }

    override suspend fun insertTasklist(tasklist: Tasklist) {
        allTasklists.add(tasklist)
    }

    override suspend fun updateTasklist(tasklist: Tasklist) {
        val indexOfItem = allTasklists.indexOfFirst { it.uuid == tasklist.uuid }
        allTasklists[indexOfItem] = tasklist
    }

    override suspend fun deleteTasklist(tasklist: Tasklist) {
        allTasklists.removeIf { it.uuid == tasklist.uuid }
    }
}