package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.BookDatabase
import com.example.data.repository.BookRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookViewModel
import com.example.ui.viewmodel.BookViewModelFactory

class MainActivity : ComponentActivity() {

    // Initialize database components lazily
    private val database by lazy { BookDatabase.getDatabase(this) }
    private val repository by lazy { BookRepository(database.bookDao()) }
    
    private val viewModel: BookViewModel by viewModels {
        BookViewModelFactory(repository)
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                // Prepopulate standard database items on startup
                LaunchedEffect(Unit) {
                    viewModel.prepopulateSampleBooksIfEmpty(context)
                }

                val activeBook by viewModel.activeBook.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Smooth visual transition when entering or leaving reader mode
                    AnimatedContent(
                        targetState = activeBook,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ReaderScreenNavigation"
                    ) { book ->
                        if (book != null) {
                            ReaderScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                onBack = { viewModel.closeBook() }
                            )
                        } else {
                            HomeScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
