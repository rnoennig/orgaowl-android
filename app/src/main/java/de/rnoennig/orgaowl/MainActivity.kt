package de.rnoennig.orgaowl

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.AutoMigration
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
import kotlinx.coroutines.flow.flowOf
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
    @ColumnInfo(name = "extra", defaultValue = "") val extra: String = "",
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
    val tasks: List<Task> = listOf()
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

@Database(
    entities = [Task::class, Tasklist::class],
    version = 2,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ],
    exportSchema = true
)
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

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: ITaskRepository
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
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.insertTask(task)

            taskRepository.getAllTasklists().collect() { allTasklists ->
                _uiState.update { it.copy(
                    availableTasklists = allTasklists
                ) }
            }
            /*
            val currentTasklist = uiState.value.availableTasklists?.find { it.tasklist.uuid == uiState.value.currentList }
            _uiState.update { it.copy(
                availableTasklists = it.availableTasklists?.map {
                    if (it.tasklist.uuid == currentTasklist?.tasklist?.uuid) {
                        it.copy(tasks = it.tasks + task)
                    } else {
                        it
                    }
                }
            )}
            */
        }

    fun updateTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateTask(task)
            val currentTasklist = uiState.value.availableTasklists?.find { it.tasklist.uuid == uiState.value.currentList }

            _uiState.update { it.copy(
                availableTasklists = uiState.value.availableTasklists?.map {
                    if (it.tasklist.uuid == currentTasklist?.tasklist?.uuid) {
                        it.copy(tasks = it.tasks.map {
                            if (it.uuid == task.uuid) {
                                task
                            } else {
                                it
                            }
                        })
                    } else {
                        it
                    }
                }
            )}

            /*
            taskRepository.getAllTasklists().collect() { allTasklists ->
                _uiState.update { it.copy(
                    availableTasklists = allTasklists
                )}
            }
            */

        }

    fun deleteTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) { taskRepository.deleteTask(task) }

    fun addTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.insertTasklist(tasklist)
            taskRepository.getAllTasklists().collect() { allTasklists ->
                _uiState.update { it.copy(
                    currentList = tasklist.uuid,
                    availableTasklists = allTasklists
                ) }
            }
        }
    }

    fun updateTasklist(updatedTaskList: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateTasklist(updatedTaskList)
            taskRepository.getAllTasklists().collect() { allTasklists ->
                _uiState.update { it.copy(
                    availableTasklists = allTasklists
                )}
            }
        }
    }

    fun deleteTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTasklist(tasklist)
            taskRepository.getAllTasklists().collect() { allTasklists ->
                _uiState.update { it.copy(
                    currentList = allTasklists.first().tasklist.uuid,
                    availableTasklists = allTasklists
                )}
            }
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
            AppDatabase::class.java, "orgaowl",

        )
            .build()

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.taskDao()

    @Provides
    fun provideTasklistDao(appDatabase: AppDatabase): TasklistDao = appDatabase.tasklistDao()

    @Provides
    fun provideItemRepository(taskDao: TaskDao, tasklistDao: TasklistDao): ITaskRepository =
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
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    OrgaOwlTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TasklistsDetailsView(
                state,
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
    uiState: State<TasklistUiState>,
    onChangeCurrentTasklist: (UUID) -> Unit,
    onTaskAdd: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onTasklistAdd: (Tasklist) -> Unit,
    onTasklistUpdate: (Tasklist) -> Unit,
    onTasklistDelete: (Tasklist) -> Unit,
    modifier: Modifier = Modifier
) {
    // Model
    val currentTasklist = uiState.value.availableTasklists?.find { it.tasklist.uuid == uiState.value.currentList }
    val taskList =
            currentTasklist?.tasks.orEmpty()
                .sortedWith(compareBy<Task> { it.done }.thenByDescending { it.modifiedAt })

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
    val listState = rememberLazyListState()
    val fabVisibility by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
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
                        uiState.value.availableTasklists?.forEach { availableTasklist ->
                            val onNavClick = {
                                onChangeCurrentTasklist.invoke(availableTasklist.tasklist.uuid)
                                scope.launch { drawerState.close() }
                            }
                            NavigationDrawerItem(
                                label = { Text(text = availableTasklist.tasklist.name) },
                                selected = availableTasklist.tasklist.uuid == uiState.value.currentList,
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
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxSize(),
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
                                text = (if (currentTasklist != null) currentTasklist?.tasklist?.name else "") + if (uiState.value.isLoading) " (loading...)" else ""
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
                uiState.value.currentList?.let {
                    val density = LocalDensity.current
                    AnimatedVisibility(
                        modifier = modifier,
                        visible = fabVisibility,
                        enter = slideInVertically {
                            with(density) { 40.dp.roundToPx() }
                        } + fadeIn(),
                        exit = fadeOut(
                            animationSpec = keyframes {
                                this.durationMillis = 120
                            }
                        )
                    ) {
                        FloatingActionButton(
                            onClick = {
                                showAddTaskDialog.value = true
                                addUpdateTaskDialogTask.value =
                                    Task(tasklist = uiState.value.currentList!!, name = "")
                                addUpdateTaskDialogCallback.value = { newTask: Task ->
                                    // move new task at the top or at the top of done tasks
                                    //val newIdx = getInsertionIndex(newTask, taskList)
                                    //taskList.add(newIdx, newTask)

                                    onTaskAdd.invoke(
                                        newTask.copy(
                                            tasklist = newTask.tasklist,
                                            name = newTask.name,
                                            extra = newTask.extra,
                                            modifiedAt = System.currentTimeMillis()
                                        )
                                    )
                                    null
                                }
                            }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add item")
                        }
                    }
                }
            }
        ) { innerPadding ->
            TasksListView(
                taskList,
                onTaskClick = {
                    //val idx = taskList.indexOf(it)
                    //val new = taskList.get(idx)


                    // update task via viewmodel
                    onUpdateTask.invoke(it.copy(done = !it.done, modifiedAt = System.currentTimeMillis()))

                    // TODO rest might be unnecessary if a stable sort algo is used

                    // first remove the clicked task
                    //taskList.removeAt(idx)

                    // then insert task at the correct pos to preserve the rest of the listitem views
                    //val newIdx = getInsertionIndex(new, taskList)
                    //taskList.add(newIdx, new)
                },
                onTaskLongClick = {
                    showUpdateTaskDialog.value = true
                    addUpdateTaskDialogTask.value = it
                    addUpdateTaskDialogCallback.value = { task ->
                        //val idx = taskList.indexOfFirst { it.uuid == task.uuid }
                        onUpdateTask.invoke(task)
                        //taskList.set(idx, task)

                        null
                    }
                },
                listState = listState,
                modifier = modifier
                    .padding(innerPadding)
            )
            if (showAddTaskDialog.value || showUpdateTaskDialog.value) {
                DialogAddUpdateTask(
                    uiState.value,
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
                    uiState.value,
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
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = listState,
        modifier = modifier
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
            if (index < taskList.lastIndex)
                Divider(thickness = 1.dp)
        }
    }
}

/**
 * Renders a single task item in the list
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
                    fontSize = 24.sp,
                    color = if (task.done) LocalTextStyle.current.color.compositeOver(Color.Gray) else LocalTextStyle.current.color
                )
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = task.extra,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(80.dp)
                    .wrapContentHeight(),
                style = LocalTextStyle.current.copy(
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    fontSize = 24.sp,
                    color = if (task.done) LocalTextStyle.current.color.compositeOver(Color.Gray) else LocalTextStyle.current.color
                )
            )
        }

    }
}

/**
 * Create a new task with the input
 */
@OptIn(ExperimentalComposeUiApi::class)
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
    val submitTask: () -> Unit = {
        onSubmitTask.invoke(task.copy(name = taskName.value, extra = taskExtra.value, done = addDoneTask.value))
    }
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
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTask.invoke() }),
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
                        placeholder = { Text("Enter extra infos") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTask.invoke() })
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
                            submitTask.invoke()
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
    val submitTasklist: () -> Unit = {
        onSubmitTask(tasklist.copy(name = tasklistName.value))
    }
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
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTasklist.invoke() }),
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
                            submitTasklist.invoke()
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
