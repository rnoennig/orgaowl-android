package de.rnoennig.orgaowl.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rnoennig.orgaowl.persistence.Task

/**
 * Shows the scrollable list of tasks
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksListView(
    taskList: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
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
                onLongClick = { onTaskLongClick(task) },
                onTaskMove = {},
                onTaskDelete = { onTaskDelete(task) }
            )
            if (index < taskList.lastIndex)
                HorizontalDivider(thickness = 1.dp)
        }
    }
}