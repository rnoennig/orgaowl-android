package de.rnoennig.orgaowl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.rnoennig.orgaowl.persistence.Tasklist

@Composable
fun DialogAddUpdateTasklist(
    tasklist: Tasklist,
    isNewTasklist: Boolean,
    onDismissRequest: () -> Unit,
    onSubmitTask: (Tasklist) -> Unit
) {
    val tasklistName = remember { mutableStateOf(tasklist.name) }
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
                    OutlinedTextField(
                        value = tasklistName.value,
                        onValueChange = { tasklistName.value = it },
                        label = { Text("List name") },
                        placeholder = { Text("Enter list name") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
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