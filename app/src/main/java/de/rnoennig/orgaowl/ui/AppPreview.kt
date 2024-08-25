package de.rnoennig.orgaowl.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import de.rnoennig.orgaowl.model.ListBackedTaskRepository
import de.rnoennig.orgaowl.model.TaskViewModel
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import java.util.UUID

@MyPreview
@Composable
fun AppPreview() {
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
    viewModel.initialize()
    App(viewModel)
}