package de.rnoennig.orgaowl.model

import de.rnoennig.orgaowl.persistence.TasklistWithTasks
import java.util.UUID

data class TasklistUiState(
    val availableTasklists: List<TasklistWithTasks>? = null,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val currentList: UUID? = null
)