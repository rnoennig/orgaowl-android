package de.rnoennig.orgaowl

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
    @ColumnInfo(name = "extra") val extra: String = "",
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
    abstract fun updateTasklist(tasklist: Tasklist)

    @Delete
    abstract fun deleteTasklist(tasklist: Tasklist)
}

@Database(entities = [Task::class, Tasklist::class], version = 2)
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
    private val tasklistDao: TasklistDao
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getAllTasklists(): Flow<List<TasklistWithTasks>> = tasklistDao.getAllTasklistsWithTasks()

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun insertTasklist(tasklist: Tasklist) = tasklistDao.insertTasklist(tasklist)
    suspend fun updateTasklist(tasklist: Tasklist) = tasklistDao.updateTasklist(tasklist)
    suspend fun deleteTasklist(tasklist: Tasklist) = tasklistDao.deleteTasklist(tasklist)
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
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.getAllTasklists().collect { listOftasklists ->
                _uiState.update { it.copy(
                    isLoading = false,
                    availableTasklists = listOftasklists,
                    currentList = if (uiState.value.currentList != null) uiState.value.currentList else listOftasklists.firstOrNull()?.tasklist?.uuid
                )}
            }
        }
    }

    fun addTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) { taskRepository.insertTask(task) }

    fun updateTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) { taskRepository.updateTask(task) }

    fun deleteTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) { taskRepository.deleteTask(task) }

    fun addTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.insertTasklist(tasklist)
            val availableTasklists = uiState.value.availableTasklists.orEmpty().toMutableList()
            availableTasklists.add(TasklistWithTasks(tasklist = tasklist, tasks = listOf()))
            _uiState.update { it.copy(availableTasklists = availableTasklists) }
        }
    }

    fun updateTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateTasklist(tasklist)
        }
    }

    fun deleteTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTasklist(tasklist)
            _uiState.update { it.copy(
                currentList = uiState.value.availableTasklists?.firstOrNull()?.tasklist?.uuid
            )}
        }
    }

    fun setCurrentTasklist(newActiveTasklistUUID: UUID) {
        _uiState.update { it.copy(currentList = newActiveTasklistUUID) }
    }

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
            .build()

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.taskDao()

    @Provides
    fun provideTasklistDao(appDatabase: AppDatabase): TasklistDao = appDatabase.tasklistDao()

    @Provides
    fun provideItemRepository(taskDao: TaskDao, tasklistDao: TasklistDao): TaskRepository =
        TaskRepository(taskDao, tasklistDao)
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
                onChangeCurrentTasklist = { newActiveTasklistUUID ->
                    viewModel.setCurrentTasklist(newActiveTasklistUUID)
                },
                onTaskAdd = { newTask ->
                    viewModel.addTask(newTask)
                },
                onUpdateTask = { newTaskDetails ->
                    viewModel.updateTask(newTaskDetails)
                },
                onTasklistAdd = { newTasklist ->
                    viewModel.addTasklist(newTasklist)
                },
                onTasklistUpdate = { newTasklist ->
                    viewModel.updateTasklist(newTasklist)
                },
                onTasklistDelete = { tasklist ->
                    viewModel.deleteTasklist(tasklist)
                },
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
    onChangeCurrentTasklist: (UUID) -> Unit,
    onTaskAdd: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onTasklistAdd: (Tasklist) -> Unit,
    onTasklistUpdate: (Tasklist) -> Unit,
    onTasklistDelete: (Tasklist) -> Unit,
    modifier: Modifier = Modifier
) {
    // Model
    val currentTasklist = uiState.availableTasklists?.find { it.tasklist.uuid == uiState.currentList }
    val taskList =
            currentTasklist?.tasks.orEmpty()
                .sortedWith(compareBy<Task> { it.done }.thenByDescending { it.modifiedAt })
                .toMutableStateList()

    // Local UI State
    val showAddTaskDialog = remember { mutableStateOf(false) }
    val showUpdateTaskDialog = remember { mutableStateOf(false) }
    val showAddTasklistDialog = remember { mutableStateOf(false) }
    val showUpdateTasklistDialog = remember { mutableStateOf(false) }
    val addUpdateTaskDialogTask = remember { mutableStateOf(Task()) }
    val addUpdateTasklistDialogTasklist = remember { mutableStateOf(Tasklist()) }
    val addUpdateTaskDialogCallback = remember { mutableStateOf({ task: Task -> null }) }
    val addUpdateTasklistDialogCallback = remember { mutableStateOf({ task: Tasklist -> null }) }
    val showDropDown = remember { mutableStateOf(false) }
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Lists",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(20.dp)
                    )
                    Divider()
                    Column {
                        uiState.availableTasklists?.forEach { availableTasklist ->
                            val onNavClick = {
                                onChangeCurrentTasklist.invoke(availableTasklist.tasklist.uuid)
                                scope.launch { drawerState.close() }
                            }
                            NavigationDrawerItem(
                                label = { Text(text = availableTasklist.tasklist.name) },
                                selected = availableTasklist.tasklist.uuid == uiState.currentList,
                                onClick = {
                                    onChangeCurrentTasklist.invoke(availableTasklist.tasklist.uuid)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "",
                        modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxSize(),
                        alignment = Alignment.BottomCenter
                    )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Show available lists"
                                )
                            }
                            Text(
                                text = (if (currentTasklist != null) currentTasklist?.tasklist?.name else "") + if (uiState.isLoading) " (loading...)" else ""
                            )
                            DropdownMenu(
                                expanded = showDropDown.value,
                                onDismissRequest = { showDropDown.value = false },
                                modifier = Modifier.width(180.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Create new list",
                                            fontSize = 18.sp
                                        )
                                    },
                                    onClick = {
                                        addUpdateTasklistDialogTasklist.value = Tasklist(name = "")
                                        addUpdateTasklistDialogCallback.value = { newTasklist: Tasklist ->
                                            onTasklistAdd.invoke(newTasklist)
                                            null
                                        }
                                        showAddTasklistDialog.value = true
                                        showDropDown.value = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Rename list",
                                            fontSize = 18.sp
                                        )
                                    },
                                    enabled = currentTasklist?.tasklist != null,
                                    onClick = {
                                        addUpdateTasklistDialogTasklist.value = currentTasklist?.tasklist!!
                                        addUpdateTasklistDialogCallback.value = { newTasklist: Tasklist ->
                                            onTasklistUpdate.invoke(newTasklist)
                                            null
                                        }
                                        showUpdateTasklistDialog.value = true
                                        showDropDown.value = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete list",
                                            fontSize = 18.sp
                                        )
                                    },
                                    enabled = currentTasklist?.tasklist != null,
                                    onClick = {
                                        onTasklistDelete.invoke(currentTasklist?.tasklist!!)
                                        showDropDown.value = false
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDropDown.value = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Quick actions"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                uiState.currentList?.let {
                    FloatingActionButton(
                        onClick = {
                        showAddTaskDialog.value = true
                        addUpdateTaskDialogTask.value = Task(tasklist = uiState.currentList!!, name = "")
                        addUpdateTaskDialogCallback.value = { newTask: Task ->
                            // move new task at the top or at the top of done tasks
                            val newIdx = getInsertionIndex(newTask, taskList)
                            taskList.add(newIdx, newTask)

                            onTaskAdd.invoke(newTask)

                            null
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add item")
                    }
                }
            }
        ) { innerPadding ->
            TasksListView(
                taskList,
                onTaskClick = {
                    val idx = taskList.indexOf(it)
                    val new = taskList.get(idx)
                        .copy(done = !it.done, modifiedAt = System.currentTimeMillis())

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
                    showAddTaskDialog.value,
                    onDismissRequest = {
                        showAddTaskDialog.value = false
                        showUpdateTaskDialog.value = false
                    },
                    onSubmitTask = {
                        addUpdateTaskDialogCallback.value.invoke(it)
                        showAddTaskDialog.value = false
                        showUpdateTaskDialog.value = false
                    })
            }
            if (showAddTasklistDialog.value || showUpdateTasklistDialog.value) {
                DialogAddUpdateTasklist(
                    uiState,
                    addUpdateTasklistDialogTasklist.value,
                    showAddTasklistDialog.value,
                    onDismissRequest = {
                        showAddTasklistDialog.value = false
                        showUpdateTasklistDialog.value = false
                    },
                    onSubmitTask = {
                        addUpdateTasklistDialogCallback.value.invoke(it)
                        showAddTasklistDialog.value = false
                        showUpdateTasklistDialog.value = false
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        itemsIndexed(
            items = taskList,
            key = { index, task -> task.uuid }
        ) { index, task ->
            ListItem(
                task,
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) }
            )
            if (index < taskList.lastIndex && index > 0)
                Divider(thickness = 1.dp)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
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
        Row {
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
            Spacer(Modifier.weight(1f))
            Text(
                text = task.extra,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(80.dp)
                    .wrapContentHeight()
            )
        }

    }
}

/**
 * Create a new task with the input
 */
@Composable
fun DialogAddUpdateTask(
    uiState: TasklistUiState,
    task: Task,
    isNewTask: Boolean ,
    onDismissRequest: () -> Unit,
    onSubmitTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val addDoneTask = remember { mutableStateOf(task.done) }
    var taskName = remember { mutableStateOf(task.name) }
    var taskExtra = remember { mutableStateOf(task.extra) }
    val focusRequester = FocusRequester()
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.padding(6.dp),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(value = taskName.value,
                        onValueChange = { taskName.value = it },
                        label = { Text("Item name") },
                        placeholder = { Text("Enter Item name") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(value = taskExtra.value,
                        onValueChange = { taskExtra.value = it },
                        label = { Text("Extra") },
                        placeholder = { Text("Enter extra infos") }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSubmitTask(task.copy(name = taskName.value, extra = taskExtra.value, done = addDoneTask.value))
                        }
                    ) {
                        Text(
                            text = if (isNewTask) "Create item" else "Update item"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogAddUpdateTasklist(
    uiState: TasklistUiState,
    tasklist: Tasklist,
    isNewTasklist: Boolean,
    onDismissRequest: () -> Unit,
    onSubmitTask: (Tasklist) -> Unit,
    modifier: Modifier = Modifier
) {
    var tasklistName = remember { mutableStateOf(tasklist.name) }
    val focusRequester = FocusRequester()
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.padding(6.dp),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(value = tasklistName.value,
                        onValueChange = { tasklistName.value = it },
                        label = { Text("List name") },
                        placeholder = { Text("Enter list name") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSubmitTask(tasklist.copy(name = tasklistName.value))
                        }
                    ) {
                        Text(
                            text = if (isNewTasklist) "Create list" else "Update list"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultPreview() {
    val uuid = UUID.randomUUID()
    val uiState = TasklistUiState(
        currentList = uuid,
        availableTasklists = listOf(
            TasklistWithTasks(
                tasklist = Tasklist(uuid = uuid, name = "Einkaufen"),
                tasks = listOf<Task>(
                    Task(name = "Möhren", extra = "1kg"),
                    Task(name = "Äpfel", extra = "3"),
                    Task(name = "Taschentücher", extra = "1")
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
        )
    )
    OrgaOwlTheme {
        TasklistsDetailsView(uiState, {}, {}, {}, {}, {}, {})
    }
}
