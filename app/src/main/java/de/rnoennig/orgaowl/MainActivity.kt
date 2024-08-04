package de.rnoennig.orgaowl

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import de.rnoennig.orgaowl.model.TaskViewModel
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.TaskDao
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.persistence.TasklistDao
import de.rnoennig.orgaowl.persistence.TasklistWithTasks
import de.rnoennig.orgaowl.ui.App
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

data class TasklistUiState(
    val availableTasklists: List<TasklistWithTasks>? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val currentList: UUID? = null
)

interface ITaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getAllTasklists(): Flow<List<TasklistWithTasks>>

    suspend fun insertTask(task: Task)

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)

    suspend fun insertTasklist(tasklist: Tasklist)

    suspend fun updateTasklist(tasklist: Tasklist)

    suspend fun deleteTasklist(tasklist: Tasklist)
}

class TaskRepository(
    private val taskDao: TaskDao,
    private val tasklistDao: TasklistDao
) : ITaskRepository {
    override fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    override fun getAllTasklists(): Flow<List<TasklistWithTasks>> = tasklistDao.getAllTasklistsWithTasks()

    override suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    override suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    override suspend fun insertTasklist(tasklist: Tasklist) = tasklistDao.insertTasklist(tasklist)
    override suspend fun updateTasklist(tasklist: Tasklist) = tasklistDao.updateTasklist(tasklist)
    override suspend fun deleteTasklist(tasklist: Tasklist) = tasklistDao.deleteTasklist(tasklist)
}

@HiltAndroidApp
class OrgaOwlApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        val viewModel: TaskViewModel by viewModels()
       lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    setContent {
                        App(viewModel)
                    }
                }
            }
        }

        /*

        // here?
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "orgaowl"
        ).build()

        val taskDao = db.taskDao()
        val taskListFromDB: List<Task> = taskDao.getAll()


        for (task: Task in taskListFromDB) {
            taskList.add(TaskTO(uid = task.uid, name = task.name, done = task.done))
        }*/


    }
}

class ListBackedTaskRepository(
    private val allTasklists: MutableList<Tasklist> = mutableListOf(),
    private val allTasks: MutableList<Task> = mutableListOf()
) : ITaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
            return flowOf(allTasks)
        }

        override fun getAllTasklists(): Flow<List<TasklistWithTasks>> {
            return flowOf(allTasklists.map {  tasklist ->
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultPreview() {
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val allTasklists = mutableListOf<Tasklist>(
        Tasklist(uuid = uuid1, name = "Einkaufen"),
        Tasklist(uuid = uuid2, name = "Asiamarkt")
    )
    val allTasks = mutableListOf<Task>(
                Task(tasklist = uuid1, name = "Möhren", extra = "1kg"),
                Task(tasklist = uuid1, name = "Äpfel", extra = "3"),
                Task(tasklist = uuid1, name = "Taschentücher", extra = "1"),
                Task(tasklist = uuid1, name = "Trauben", extra = "500g"),
                Task(tasklist = uuid1, name = "Knäckebrot", extra = ""),
                Task(tasklist = uuid1, name = "Zimt", extra = ""),
                Task(tasklist = uuid1, name = "Kartoffeln", extra = "1kg"),
                Task(tasklist = uuid1, name = "Zucchini", extra = ""),
                Task(tasklist = uuid1, name = "Tomaten", extra = "3"),
                Task(tasklist = uuid1, name = "Erdbeeren", extra = "500g"),
                Task(tasklist = uuid1, name = "Mandeln", extra = "100g"),
                Task(tasklist = uuid1, name = "Kohlrabi", extra = "1"),

                Task(tasklist = uuid2, name = "Sesamöl"),
                Task(tasklist = uuid2, name = "Sojasoße"),
                Task(tasklist = uuid2, name = "Tofu")
    )
    val taskRepository = ListBackedTaskRepository(allTasklists = allTasklists, allTasks = allTasks)
    val viewModel = TaskViewModel(taskRepository = taskRepository, savedStateHandle = SavedStateHandle())
    App(viewModel)
}
