package de.rnoennig.orgaowl

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.WorkerThread
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.asLiveData
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.rnoennig.orgaowl.ui.theme.OrgaOwlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class TaskDetails(
    var uid: Int,
    var name: String = "Unnamed Task",
    var done: Boolean = false,
    var extra: String = "extra"
)

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "done") val done: Boolean
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun getAllTasks(): Flow<List<Task>>

    @Insert
    fun insertTask(task: Task)

    @Update
    fun updateTask(task: Task)

    @Delete
    fun deleteTask(task: Task)
}

@Database(entities = [Task::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}


data class TasklistUiState(
    val taskList: List<Task> = listOf(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
) {

}

class TaskRepository(private val taskDao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasklistUiState())
    val uiState: StateFlow<TasklistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy( isLoading = true) }
            taskRepository.getAllTasks().collect { listOfItems ->
                _uiState.update { it.copy( isLoading = false, taskList = listOfItems) }
            }
        }
    }

    fun fetchTasks() = {
        viewModelScope.launch {
            _uiState.update { it.copy( isLoading = true) }
            taskRepository.getAllTasks().collect { listOfItems ->
                _uiState.update { it.copy( isLoading = false, taskList = listOfItems) }
            }
        }
    }

    fun addTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { taskRepository.insertTask(task) }

    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { taskRepository.updateTask(task) }

    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) { taskRepository.deleteTask(task) }

}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java, "orgaowl"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.taskDao()

    @Provides
    fun provideItemRepository(taskDao: TaskDao): TaskRepository = TaskRepository(taskDao)
}

@HiltAndroidApp
class OrgaOwlApplication : Application() {

}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        val taskList2 = mutableListOf<TaskDetails>()
        val viewModel: TaskViewModel by viewModels()
        /*        lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        val taskList = listOf<TaskDetails>(
                            TaskDetails(1, "Wäsche", false),
                            TaskDetails(2, "Android compose lernen", true),
                            TaskDetails(3, "Android compose lernen 2", false),
                            TaskDetails(4, "Android compose lernen 3", false),
                            TaskDetails(11)
                        )
                        //viewModel.updateTasks(taskList)
                        viewModel.uiState.collect {
                            // Update UI elements
                        }
                    }
                }*/

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

        setContent {
            App(viewModel)
        }
    }
}


/**
 * Entry point, loads data and loads main screen
 */
@Composable
fun App(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    OrgaOwlTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TasklistsDetailsView(
                viewModel
            )
        }
    }
}

fun getInsertionIndex(newTask: Task, taskList: List<Task>): Int {
    // if task was done, re-add it to the top of the list
    var newIdx = 0
    if (newTask.done) {
        // if the task was marked done move it to the top of the done tasks
        newIdx = taskList.indexOf(taskList.firstOrNull { task -> task.done })
        // if it's the first done task move it to end of the list
        if (newIdx < 0) newIdx = taskList.size
    }
    return newIdx
}

/**
 * Shows a single task list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasklistsDetailsView(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    // Model
    val state = viewModel.uiState.collectAsState()
    val taskList = remember {
        mutableStateListOf<Task>().apply {
            addAll(state.value.taskList.orEmpty().sortedBy { it.done })
        }
    }
    //val taskList = viewModel.taskList

    // UI State
    val showAddTaskDialog = remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("OrgaOwl" + if (state.value.isLoading) " (loading...)" else "" )
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog.value = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task.")
            }
        }
    ) { innerPadding ->
        TasksListView(
            taskList,
            onTaskClick = {
                val idx = taskList.indexOf(it)
                val new = taskList.get(idx).copy(done = !it.done)

                // update task via viewmodel
                viewModel.updateTask(new)

                // TODO rest might be unnecessary if a stable sort algo is used

                // first remove the clicked task
                taskList.removeAt(idx)

                // then insert task at the correct pos to preserve the rest of the listitem views
                val newIdx = getInsertionIndex(new, taskList)
                taskList.add(newIdx, new)
            },
            modifier = modifier
                .padding(innerPadding)
        )
        if (showAddTaskDialog.value) {
            DialogAddTask(
                viewModel,
                onDismissRequest = { showAddTaskDialog.value = false },
                onCreateTask = { newTask ->
                    // move new task at the top or at the top of done tasks
                    val newIdx = getInsertionIndex(newTask, taskList)
                    taskList.add(newIdx, newTask)

                    viewModel.addTask(newTask)

                    showAddTaskDialog.value = false
                })
        }
    }
}

/**
 * Shows the scrollable list of tasks
 */
@Composable
fun TasksListView(
    taskList: List<Task>,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize()) {
        items(
            items = taskList,
            key = { task -> task.uid }
        ) {
            ListItem(it,
                onClick = onTaskClick
            )
        }
    }
}

/**
 * Renders a single task item in the list
 */
@Composable
fun ListItem(task: Task, modifier: Modifier = Modifier, onClick: ((Task) -> Unit)? = null) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            ) {
                onClick?.invoke(task)
            }
            .padding(6.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(80.dp)
                .wrapContentHeight(),
            text = task.name,
            style = LocalTextStyle.current.copy(
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                fontSize = 24.sp
            )
        )
    }
}

/**
 * Create a new task with the input
 */
@Composable
fun DialogAddTask(
    viewModel: TaskViewModel,
    onDismissRequest: () -> Unit,
    onCreateTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val addDoneTask = remember { mutableStateOf(false) }
    var taskName = remember { mutableStateOf("New task") }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface (
            modifier = Modifier.padding(6.dp)
            ,
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column (
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row (
                    modifier = modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        modifier = modifier,
                        checked = addDoneTask.value,
                        onCheckedChange = { addDoneTask.value = it }
                    )
                    OutlinedTextField(value = taskName.value,
                        onValueChange = { taskName.value = it },
                        label = { Text("Task name") }
                    )
                }
                Row (
                    modifier = modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onCreateTask(Task(uid = 0, name = taskName.value, done = addDoneTask.value))
                        }
                    ) {
                        Text(
                            text = "Create task"
                        )
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val taskList = listOf<TaskDetails>(
        TaskDetails(1, "Wäsche", false),
        TaskDetails(2, "Android compose lernen", true),
        TaskDetails(3, "Android compose lernen 2", false),
        TaskDetails(4, "Android compose lernen 3", false),
        TaskDetails(5, "Android compose lernen 3", false),
        TaskDetails(6, "Android compose lernen 3", false),
        TaskDetails(7, "Android compose lernen 3", false),
        TaskDetails(8, "Android compose lernen 3", false),
        TaskDetails(9, "Android compose lernen 3", false),
        TaskDetails(10, "Android compose lernen 3", false),
        TaskDetails(11)
    )
    //TasklistsDetailsView(taskList)
}
