package org.example.project.features.blogs.presentations.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.core.ui.components.EmptyView
import org.example.project.core.ui.components.ErrorView
import org.example.project.core.ui.components.LoadingIndicator
import org.example.project.features.blogs.domain.model.BlogModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogScreen(
    viewModel: BlogViewModel = koinViewModel(),
    onBlogClick: (Int) -> Unit = {},
) {

    val state by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {

        viewModel.effect.collect { effect ->

            when (effect) {

                is BlogEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)

                is BlogEffect.NavigateToDetail ->
                    onBlogClick(effect.blogId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blogs") },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.onIntent(BlogIntent.Refresh)
                        },
                    ) {
                        Text("Refresh")
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

                BlogState.Initial,
                BlogState.Loading -> LoadingIndicator()

                is BlogState.Success -> {

                    if (s.blogs.isEmpty()) {
                        EmptyView(
                            message = "Belum ada blog",
                            actionLabel = "Reload",
                            onAction = {
                                viewModel.onIntent(BlogIntent.Refresh)
                            },
                        )
                    } else {
                        BlogList(
                            blogs = s.blogs,
                            isRefreshing = s.isRefreshing,
                            onBlogClick = { id ->
                                viewModel.onIntent(BlogIntent.BlogClicked(id))
                            },
                        )
                    }
                }

                is BlogState.Failure -> {
                    ErrorView(
                        message = s.message,
                        onRetry = {
                            viewModel.onIntent(BlogIntent.LoadBlogs)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlogList(
    blogs: List<BlogModel>,
    isRefreshing: Boolean,
    onBlogClick: (Int) -> Unit,
) {

    Column {

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            items(
                items = blogs,
                key = { it.id },
            ) { blog ->

                BlogItem(
                    blog = blog,
                    onClick = { onBlogClick(blog.id) },
                )
            }
        }
    }
}

@Composable
private fun BlogItem(
    blog: BlogModel,
    onClick: () -> Unit,
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            Text(
                text = blog.title,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = blog.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}
