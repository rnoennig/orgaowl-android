package de.rnoennig.orgaowl.ui

import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.rnoennig.orgaowl.persistence.Task
import de.rnoennig.orgaowl.ui.theme.OrgaOwlTheme
import java.io.File
import kotlin.math.roundToInt

enum class DragValue { Start, Center }

/**
 * Renders a single task item in the list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItem(
    task: Task,
    onClick: ((Task) -> Unit)? = null,
    onLongClick: ((Task) -> Unit)? = null,
    onTaskDelete: ((Task) -> Unit)? = null
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    val anchors = with(LocalDensity.current) {
            DraggableAnchors {
                DragValue.Start at -50.dp.toPx()
                DragValue.Center at 0f
            }
        }

    val state = remember { AnchoredDraggableState(
        DragValue.Center,
        anchors = anchors,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 100.dp.toPx() } },
        animationSpec = tween(),
        confirmValueChange = { newValue ->
            when(newValue) {
                DragValue.Start -> {
                    onTaskDelete?.invoke(task)
                }
                DragValue.Center -> Unit
            }
            true
        }
    ) }
    SideEffect {
        state.updateAnchors(anchors)
    }

    Box {
        Row {
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { onTaskDelete?.invoke(task) },
                Modifier.background(MaterialTheme.colorScheme.onError)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "Delete"
                )
            }
        }
        // above is hidden and only revealed upon sliding
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
                    IntOffset(x = state.requireOffset().roundToInt(), y = 0)
                }
                .anchoredDraggable(state, Orientation.Horizontal, reverseDirection = false)
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
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewListView() {
    OrgaOwlTheme {
        ListItem(
            task = Task(name = "Banana"),
        )
    }
}
