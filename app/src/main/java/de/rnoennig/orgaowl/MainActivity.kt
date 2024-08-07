package de.rnoennig.orgaowl

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import de.rnoennig.orgaowl.model.TaskViewModel
import de.rnoennig.orgaowl.ui.App

@HiltAndroidApp
class OrgaOwlApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: TaskViewModel by viewModels()
        viewModel.initialize()
        setContent {
            App(viewModel)
        }
    }
}

