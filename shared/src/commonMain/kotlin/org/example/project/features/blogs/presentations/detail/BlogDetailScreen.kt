package org.example.project.features.blogs.presentations.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.core.ui.components.ErrorView
import org.example.project.core.ui.components.LoadingIndicator
import org.example.project.features.blogs.domain.model.BlogModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    blogId: Int,
    viewModel: BlogDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(blogId) {
        viewModel.onIntent(BlogDetailIntent.LoadBlog(blogId))
    }

    LaunchedEffect(Unit) {

        viewModel.effect.collect { effect ->

            when (effect) {

                is BlogDetailEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blog Detail") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            when (val s = state) {

                BlogDetailState.Initial,
                BlogDetailState.Loading -> LoadingIndicator()

                is BlogDetailState.Success -> BlogDetailContent(blog = s.blog)

                is BlogDetailState.Failure -> {
                    ErrorView(
                        message = s.message,
                        onRetry = {
                            viewModel.onIntent(BlogDetailIntent.Retry)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlogDetailContent(
    blog: BlogModel,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        Text(
            text = "#${blog.id} • User ${blog.userId}",
            style = MaterialTheme.typography.labelMedium,
        )

        Text(
            text = blog.title,
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = blog.body,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
