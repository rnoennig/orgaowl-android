package de.rnoennig.orgaowl.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import de.rnoennig.orgaowl.BuildConfig
import de.rnoennig.orgaowl.persistence.Task
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream


/**
 * Create a new task with the input
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalPermissionsApi::class)
@Composable
fun DialogAddUpdateTask(
    task: Task,
    isNewTask: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onSubmitTask: (Task) -> Unit = {}
) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val addDoneTask = remember { mutableStateOf(task.done) }
    val taskName = remember { mutableStateOf(task.name) }
    val taskExtra = remember { mutableStateOf(task.extra) }
    val taskImagePath = remember { mutableStateOf(task.imagePath) }
    val focusRequester = FocusRequester()
    val submitTask: () -> Unit = {
        onSubmitTask.invoke(task.copy(
            name = taskName.value,
            extra = taskExtra.value,
            done = addDoneTask.value,
            imagePath = taskImagePath.value
        ))
    }
    val showImageGallery = remember { mutableStateOf(false) }
    val imageGallerySearchTerm = remember { derivedStateOf { taskName.value } }
    val webImages = remember { mutableListOf<WebImage>() }
    val imageSearchStatus = remember { mutableStateOf(ImageSearchStatus.DONE) }

    if (showImageGallery.value) {
        Dialog(onDismissRequest = { showImageGallery.value = false }) {
            Surface(
                modifier = Modifier.padding(6.dp),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = when (imageSearchStatus.value) {
                                ImageSearchStatus.PENDING -> "Waiting..."
                                ImageSearchStatus.DONE -> "Search results:"
                            },
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        when (imageSearchStatus.value) {
                            ImageSearchStatus.PENDING -> CircularProgressIndicator()
                            ImageSearchStatus.DONE -> {
                                LazyVerticalGrid(
                                    // on below line we are setting the
                                    // column count for our grid view.
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),

                                    // on below line we are adding padding
                                    // from all sides to our grid view.
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    items(webImages.size) {
                                        AsyncImage(
                                            model = webImages[it].url,
                                            contentDescription = imageGallerySearchTerm.value,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .clickable {
                                                    // download image
                                                    downloadPreviewableImage(
                                                        url = webImages[it].url,
                                                        imageFileName = imageGallerySearchTerm.value + ".png",
                                                        onSuccess = { imageFileName ->
                                                            taskImagePath.value = imageFileName
                                                            showImageGallery.value = false
                                                        },
                                                        context = context,
                                                        inspectionMode = inspectionMode
                                                    )
                                                }
                                                .size(100.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }
    } else {
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
                    val internetPermissionState = rememberPreviewablePermissionState(
                        permission = Manifest.permission.INTERNET
                    )
                    if (internetPermissionState.status == PermissionStatus.Granted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val imageMod = Modifier
                                .size(100.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                                .clickable(
                                    onClick = {
                                        imageSearchStatus.value = ImageSearchStatus.PENDING
                                        searchImages(
                                            searchTerm = imageGallerySearchTerm.value,
                                            onSuccess = {
                                                webImages.clear()
                                                webImages.addAll(adaptQueryResultToWebImages(it))
                                                imageSearchStatus.value = ImageSearchStatus.DONE
                                            },
                                            inspectionMode = inspectionMode
                                        )
                                        showImageGallery.value = true
                                    }
                                )
                            if (taskImagePath.value.isNotEmpty()) {
                                AsyncImage(
                                    model = File(context.filesDir, taskImagePath.value),
                                    contentDescription = taskName.value,
                                    modifier = imageMod,
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Image(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Edit image",
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                                    modifier = imageMod
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = taskName.value,
                            onValueChange = { taskName.value = it },
                            label = { Text("Item name") },
                            placeholder = { Text("Enter Item name") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitTask.invoke() }),
                            modifier = Modifier.focusRequester(focusRequester)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = taskExtra.value,
                            onValueChange = { taskExtra.value = it },
                            label = { Text("Extra") },
                            placeholder = { Text("Enter extra infos") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitTask.invoke() })
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
                                submitTask.invoke()
                            }
                        ) {
                            Text(
                                text = if (isNewTask) "Create item" else "Update item"
                            )
                        }
                    }
                }
            }
        }
    }
}

fun adaptQueryResultToWebImages(imageQueryResult: SerplyImageQueryResult): List<WebImage> {
    return imageQueryResult.image_results.map {
        WebImage(it.image.src)
    }
}

fun searchImages(
    searchTerm: String,
    onSuccess: (SerplyImageQueryResult) -> Unit,
    inspectionMode: Boolean
) {
    if (inspectionMode) {
        onSuccess(
            SerplyImageQueryResult(
                List<ImageQueryResultImageResult>(25) {
                    ImageQueryResultImageResult(
                        ImageQueryResultImageResultImage("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQoaiMsccAR-V6pu_1siUw3W_38bZ9xA-5Z20qn1FVQ40JyAlWfEgKanDybVA&s")
                    )
                }
            )
        )
    } else {
        val retrofit = Retrofit
            .Builder()
            .baseUrl("https://api.serply.io/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    val currentRequest = chain.request().newBuilder()
                    chain.proceed(currentRequest
                        .header("X-Api-Key", BuildConfig.SERPLY_API_KEY)
                        .build())
                })
                .build()
            )
            .build()
        val service = retrofit.create(SerplyService::class.java)
        val request = service.queryImages(searchTerm)
        request.enqueue(object : Callback<SerplyImageQueryResult> {
            override fun onResponse(
                call: Call<SerplyImageQueryResult>,
                response: Response<SerplyImageQueryResult>
            ) {
                if (response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    Log.e("DialogAddUpdateTask","Error while parsing image query result: "  + response.errorBody()?.string())
                }
            }

            override fun onFailure(
                call: Call<SerplyImageQueryResult>,
                t: Throwable
            ) {
                Log.e("DialogAddUpdateTask","Error while querying image query api", t)
            }

        })
    }
}

enum class ImageSearchStatus {
    PENDING, DONE
}

data class ImageQueryResultImageResultImage (
    val src: String
)

data class ImageQueryResultImageResult (
    val image: ImageQueryResultImageResultImage
)

data class SerplyImageQueryResult (val image_results:List<ImageQueryResultImageResult>)
interface SerplyService {
    @GET("image/q={query}")
    fun queryImages(@Path("query") imageSearchTerm: String): Call<SerplyImageQueryResult>
}

fun downloadPreviewableImage(
    url: String,
    imageFileName: String,
    onSuccess: (String) -> Unit,
    context: Context,
    inspectionMode: Boolean
) {
    if (inspectionMode) {
        onSuccess(imageFileName)
    } else {
        val imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .build()
        val imageRequest = ImageRequest.Builder(context)
            .data(url)
            .target(onSuccess = { result ->
                val file = File(context.filesDir, imageFileName)
                val fos = FileOutputStream(file)
                result.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos)

                // saved to storage
                onSuccess(imageFileName)
            })
            .build()
        imageLoader.enqueue(imageRequest)
    }
}

data class WebImage (val url: String)

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun rememberPreviewablePermissionState(permission: String): PermissionState {
    if (LocalInspectionMode.current) {
        return object : PermissionState {
            override val permission: String
                get() = TODO("Not yet implemented")
            override val status: PermissionStatus
                get() = PermissionStatus.Granted

            override fun launchPermissionRequest() {
                TODO("Not yet implemented")
            }
        }
    }
    return rememberPermissionState(
        permission = permission
    )
}

@Preview
@Composable
fun LoadingImageFromInternet() {
    DialogAddUpdateTask(
        task = Task(name = "Banana"),
    )
}
