package de.rnoennig.orgaowl.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.rnoennig.orgaowl.ITaskRepository
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.persistence.Tasklist
import de.rnoennig.orgaowl.TasklistUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

            taskRepository.getAllTasklists().collect { allTasklists ->
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
        }

    fun deleteTask(task: Task) =
        viewModelScope.launch(Dispatchers.IO) { taskRepository.deleteTask(task) }

    fun addTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.insertTasklist(tasklist)
            taskRepository.getAllTasklists().collect { allTasklists ->
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
            taskRepository.getAllTasklists().collect { allTasklists ->
                _uiState.update { it.copy(
                    availableTasklists = allTasklists
                )}
            }
        }
    }

    fun deleteTasklist(tasklist: Tasklist) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTasklist(tasklist)
            taskRepository.getAllTasklists().collect { allTasklists ->
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