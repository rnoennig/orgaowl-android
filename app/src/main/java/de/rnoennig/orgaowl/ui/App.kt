package de.rnoennig.orgaowl.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.rnoennig.orgaowl.model.ListBackedTaskRepository
import de.rnoennig.orgaowl.model.TaskViewModel
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.ui.theme.OrgaOwlTheme
import java.util.UUID

/**
 * Entry point, loads data and loads main screen
 */
@Composable
fun App(viewModel: TaskViewModel) {
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
                onDeleteTask = { task ->
                    viewModel.deleteTask(task)
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
    val viewModel =
        TaskViewModel(taskRepository = taskRepository, savedStateHandle = SavedStateHandle())
    App(viewModel)
}