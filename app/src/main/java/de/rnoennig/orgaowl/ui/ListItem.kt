package de.rnoennig.orgaowl.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.rnoennig.orgaowl.persistence.Task
import java.io.File
import kotlin.math.roundToInt

/**
 * Renders a single task item in the list
 * Renders a single task item in the list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItem(
    task: Task,
    onClick: ((Task) -> Unit)? = null,
    onLongClick: ((Task) -> Unit)? = null
) {
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
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
            if (task.imagePath.isNotEmpty()) {
                AsyncImage(
                    model = File(context.filesDir, task.imagePath),
                    contentDescription = task.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (task.imagePath.isNotEmpty() && task.done) {
                        ColorFilter.tint(Color.DarkGray, BlendMode.Multiply)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(50.dp)
                )
            }
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