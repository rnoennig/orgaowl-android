package de.rnoennig.orgaowl.model

import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.persistence.TasklistWithTasks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class ListBackedTaskRepository(
    private val allTasklists: MutableList<Tasklist> = mutableListOf(),
    private val allTasks: MutableList<Task> = mutableListOf()
) : ITaskRepository {
    var emittedAllTasklistsWithTasksFlow: MutableStateFlow<List<TasklistWithTasks>>? = null

    override fun getAllTasks(): Flow<List<Task>> {
        return flowOf(allTasks)
    }

    override fun getAllTasklists(): List<Tasklist> {
        return allTasklists
    }

    override fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>> {
        val flow = MutableStateFlow(combineTasklistsWithTasks())
        emittedAllTasklistsWithTasksFlow = flow
        return flow
    }

    private fun combineTasklistsWithTasks() : List<TasklistWithTasks> {
        return allTasklists.map { tasklist ->
            TasklistWithTasks(
                tasklist = tasklist,
                tasks = allTasks.filter { it.tasklist == tasklist.uuid })
        }
    }

    override suspend fun insertTask(task: Task) {
        allTasks.add(task)
        update()
    }

    override suspend fun updateTask(task: Task) {
        val indexOfItem = allTasks.indexOfFirst { it.uuid == task.uuid }
        allTasks[indexOfItem] = task
        update()
    }

    override suspend fun deleteTask(task: Task) {
        allTasks.removeIf { it.uuid == task.uuid }
        update()
    }

    override suspend fun insertTasklist(tasklist: Tasklist) {
        allTasklists.add(tasklist)
        update()
    }

    override suspend fun updateTasklist(tasklist: Tasklist) {
        val indexOfItem = allTasklists.indexOfFirst { it.uuid == tasklist.uuid }
        allTasklists[indexOfItem] = tasklist
        update()
    }

    override suspend fun deleteTasklist(tasklist: Tasklist) {
        allTasklists.removeIf { it.uuid == tasklist.uuid }
        update()
    }

    private suspend fun update() {
        emittedAllTasklistsWithTasksFlow?.emit(combineTasklistsWithTasks())
    }
}