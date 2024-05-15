package de.rnoennig.orgaowl

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class TaskDetails(
    var uid: Int,
    var name: String = "Unnamed Task",
    var done: Boolean = false,
    var extra: String = "extra"
)

@Entity
data class Task(
    @PrimaryKey val uuid: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "name") val name: String = "Unnamed Task",
    @ColumnInfo(name = "done") val done: Boolean = false,
    @ColumnInfo(name = "modified_at") val modifiedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "tasklist") val tasklist: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
)

@Entity
data class Tasklist(
    @PrimaryKey val uuid: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "name") val name: String = "Unnamed List",
    @ColumnInfo(name = "modified_at") val modifiedAt: Long = System.currentTimeMillis()
)

data class TasklistWithTasks(
    @Embedded val tasklist: Tasklist,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "tasklist"
    )
    val tasks: List<Task>
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

@Dao
interface TasklistDao {
    @Query("SELECT * FROM Tasklist")
    fun getAllTasklists(): Flow<List<Tasklist>>

    @Transaction
    @Query("SELECT * FROM Tasklist where name = :taskListName")
    fun getTasklistWithTasks(taskListName: String): List<TasklistWithTasks>


    @Query("SELECT * FROM Tasklist")
    fun getAllTasklistsWithTasks(): Flow<List<TasklistWithTasks>>
}

@Database(entities = [Task::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun tasklistDao(): TasklistDao
}


data class TasklistUiState(
    val availableTasklists: List<TasklistWithTasks>? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val currentList: UUID? = null
) {


}

class TaskRepository(
    private val taskDao: TaskDao,
    private val tasklistDao: TasklistDao) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getAllTasklists(): Flow<List<TasklistWithTasks>> = tasklistDao.getAllTasklistsWithTasks()

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
            taskRepository.getAllTasklists().collect { listOftasklists ->
                _uiState.update { it.copy( isLoading = false, availableTasklists = listOftasklists) }
            }
        }
    }

    fun setCurrentList(currentList: UUID) {
        _uiState.update { it.copy( currentList = currentList) }
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
    fun provideTasklistDao(appDatabase: AppDatabase): TasklistDao = appDatabase.tasklistDao()

    @Provides
    fun provideItemRepository(taskDao: TaskDao, tasklistDao: TasklistDao): TaskRepository = TaskRepository(taskDao, tasklistDao)
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
    val state = viewModel.uiState.collectAsState()
    OrgaOwlTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TasklistsDetailsView(
                state.value,
                onTaskAdd = { newTask ->
                    viewModel.addTask(newTask)
                },
                onUpdateTask = { newTaskDetails ->
                    viewModel.updateTask(newTaskDetails)
                }
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasklistsDetailsView(
    uiState: TasklistUiState,
    onTaskAdd: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    // Model
    val currentListUuid = remember { mutableStateOf(uiState.currentList) }
    val currentTasklist = uiState.availableTasklists?.find { it.tasklist.uuid == currentListUuid.value }
    val taskList = remember {
        mutableStateListOf<Task>().apply {
            addAll(currentTasklist?.tasks.orEmpty().sortedWith(compareBy<Task> { it.done }.thenByDescending { it.modifiedAt }))
        }
    }
    //val taskList = viewModel.taskList

    // UI State
    val showAddTaskDialog = remember { mutableStateOf(false) }
    val showUpdateTaskDialog = remember { mutableStateOf(false) }
    val addUpdateTaskDialogTask = remember { mutableStateOf(Task()) }
    val addUpdateTaskDialogCallback = remember { mutableStateOf({ task: Task -> null }) }
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet (
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
            ) {
                Text("Lists", modifier = Modifier.padding(22.dp))
                Divider()
                Column {
                    uiState.availableTasklists?.forEach { availableTasklist ->
                        NavigationDrawerItem(
                            label = { Text(text = availableTasklist.tasklist.name) },
                            selected = availableTasklist.tasklist.uuid == uiState.currentList,
                            onClick = {
                                currentListUuid.value = availableTasklist.tasklist.uuid
                                taskList.clear()
                                taskList.addAll(currentTasklist?.tasks.orEmpty().sortedWith(compareBy<Task> { it.done }.thenByDescending { it.modifiedAt }))

                            }
                        )
                    }
                }
            }
        },
        drawerState = drawerState
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Localized description"
                            )
                        }
                        Text(currentTasklist?.tasklist?.name + if (uiState.isLoading) " (loading...)" else "" )
                    }
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Localized description"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddTaskDialog.value = true
                addUpdateTaskDialogTask.value = Task()
                addUpdateTaskDialogCallback.value = { newTask:Task ->
                    // move new task at the top or at the top of done tasks
                    val newIdx = getInsertionIndex(newTask, taskList)
                    taskList.add(newIdx, newTask)

                    onTaskAdd.invoke(newTask)

                    null
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task.")
            }
        }
    ) { innerPadding ->
        TasksListView(
            taskList,
            onTaskClick = {
                val idx = taskList.indexOf(it)
                val new = taskList.get(idx).copy(done = !it.done, modifiedAt = System.currentTimeMillis())

                // update task via viewmodel
                onUpdateTask.invoke(new)

                // TODO rest might be unnecessary if a stable sort algo is used

                // first remove the clicked task
                taskList.removeAt(idx)

                // then insert task at the correct pos to preserve the rest of the listitem views
                val newIdx = getInsertionIndex(new, taskList)
                taskList.add(newIdx, new)
            },
            onTaskLongClick = {
                showUpdateTaskDialog.value = true
                addUpdateTaskDialogTask.value = it
                addUpdateTaskDialogCallback.value = { task ->
                    val idx = taskList.indexOfFirst { it.uuid == task.uuid }
                    onUpdateTask.invoke(task)
                    taskList.set(idx, task)

                    null
                }
            },
            modifier = Modifier
                .padding(innerPadding)
        )
        if (showAddTaskDialog.value || showUpdateTaskDialog.value) {
            DialogAddUpdateTask(
                uiState,
                addUpdateTaskDialogTask.value,
                onDismissRequest = { showAddTaskDialog.value = false },
                onSubmitTask = {
                    addUpdateTaskDialogCallback.value.invoke(it)
                    showAddTaskDialog.value = false
                    showUpdateTaskDialog.value = false
                })
        }
    }
    }
}

/**
 * Shows the scrollable list of tasks
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksListView(
    taskList: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        items(
            items = taskList,
            key = { task -> task.uuid }
        ) { task ->
            ListItem(
                task,
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) }
            )
        }
    }
}

/**
 * Renders a single task item in the list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItem(
    task: Task,
    onClick: ((Task) -> Unit)? = null,
    onLongClick: ((Task) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    Card(

        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = { onClick?.invoke(task) },
                onLongClick = { onLongClick?.invoke(task) }
            )
            .offset {
                IntOffset(
                    offsetX
                        .coerceIn(-100.dp.toPx(), 0f)
                        .roundToInt(), 0
                )
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX += delta
                }
            )
            .padding(bottom = 6.dp)
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
fun DialogAddUpdateTask(
    uiState: TasklistUiState,
    task: Task,
    onDismissRequest: () -> Unit,
    onSubmitTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val addDoneTask = remember { mutableStateOf(task.done) }
    var taskName = remember { mutableStateOf(task.name) }
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
                            onSubmitTask(task.copy(name = taskName.value, done = addDoneTask.value))
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
    val uuid = UUID.randomUUID()
    val uiState = TasklistUiState(
        availableTasklists = listOf(
            TasklistWithTasks(
                tasklist = Tasklist(uuid = uuid, name = "Einkaufen"),
                tasks = listOf<Task>(
                    Task(name = "Möhren"),
                    Task(name = "Äpfel"),
                    Task(name = "Taschentücher")
                )
            ),
            TasklistWithTasks(
                tasklist = Tasklist(name = "Asiamarkt"),
                tasks = listOf<Task>(
                    Task(name = "Sesamöl"),
                    Task(name = "Sojasoße"),
                    Task(name = "Tofu")
                )
            ),
        ),
        currentList = uuid
    )
    TasklistsDetailsView(uiState, {}, {})
}
