package de.rnoennig.orgaowl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rnoennig.orgaowl.model.TaskViewModel
import de.rnoennig.orgaowl.ui.theme.OrgaOwlTheme

/**
 * Entry point, loads data and loads main screen
 */
@Composable
fun App(
    viewModel: TaskViewModel
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