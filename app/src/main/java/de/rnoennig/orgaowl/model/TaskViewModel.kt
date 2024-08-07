package de.rnoennig.orgaowl.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: ITaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasklistUiState())
    val uiState: StateFlow<TasklistUiState> = _uiState.asStateFlow()

    fun initialize() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.getAllTasklistsWithTasks().collect { listOftasklists ->
                _uiState.update { it.copy(
                    isLoading = false,
                    availableTasklists = listOftasklists,
                    currentList = if (uiState.value.currentList != null) uiState.value.currentList else listOftasklists.firstOrNull()?.tasklist?.uuid
                )}
            }
        }
    }

    fun addTask(newTask: Task) =
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.insertTask(newTask)
        }

    fun updateTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateTask(task)
        }

    fun deleteTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTask(task)
        }

    fun addTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.insertTasklist(tasklist)
            _uiState.update { it.copy(
                currentList = tasklist.uuid
            )}
        }
    }

    fun updateTasklist(updatedTaskList: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.updateTasklist(updatedTaskList)
        }
    }

    fun deleteTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTasklist(tasklist)
            val availableTasklists = taskRepository.getAllTasklists()
            _uiState.update { it.copy(
                currentList = _uiState.value.availableTasklists?.first()?.tasklist?.uuid
            )}
        }
    }

    fun setCurrentTasklist(newActiveTasklistUUID: UUID) {
        _uiState.update { it.copy(currentList = newActiveTasklistUUID) }
    }

}