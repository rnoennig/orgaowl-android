package de.rnoennig.orgaowl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rnoennig.orgaowl.R
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.model.TasklistUiState
import kotlinx.coroutines.launch
import java.util.UUID

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
    onDeleteTask: (Task) -> Unit,
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
                HorizontalDivider()
                Column {
                    uiState.value.availableTasklists?.forEach { availableTasklist ->
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
                    colors = TopAppBarDefaults.topAppBarColors(
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
                                text = (currentTasklist?.tasklist?.name ?: "") + if (uiState.value.isLoading) " (loading...)" else ""
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
                                        addUpdateTasklistDialogCallback.value =
                                            { newTasklist: Tasklist ->
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
                                        addUpdateTasklistDialogTasklist.value =
                                            currentTasklist?.tasklist!!
                                        addUpdateTasklistDialogCallback.value =
                                            { newTasklist: Tasklist ->
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
                                            modifiedAt = System.currentTimeMillis(),
                                            imagePath = newTask.imagePath
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
                    onUpdateTask.invoke(
                        it.copy(
                            done = !it.done,
                            modifiedAt = System.currentTimeMillis()
                        )
                    )
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
                onTaskDelete = { task ->
                    onDeleteTask(task)
                },
                listState = listState,
                modifier = modifier
                    .padding(innerPadding)
            )
            if (showAddTaskDialog.value || showUpdateTaskDialog.value) {
                DialogAddUpdateTask(
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

@MyPreview
@Composable
fun TasklistsDetailsViewPreview() {
    AppPreview()
}