package org.example.project.features.blogs.presentations.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.usecases.GetBlogsUseCase

class BlogViewModel(
    private val getBlogsUseCase: GetBlogsUseCase,
) : ViewModel() {

    private val _state =
        MutableStateFlow<BlogState>(BlogState.Initial)

    val state = _state.asStateFlow()

    private val _effect = Channel<BlogEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(BlogIntent.LoadBlogs)
    }

    fun onIntent(intent: BlogIntent) {

        when (intent) {

            BlogIntent.LoadBlogs ->
                loadBlogs(isRefresh = false)

            BlogIntent.Refresh ->
                loadBlogs(isRefresh = true)

            is BlogIntent.BlogClicked ->
                viewModelScope.launch {
                    _effect.send(
                        BlogEffect.NavigateToDetail(intent.id)
                    )
                }
        }
    }

    private fun loadBlogs(isRefresh: Boolean) {

        viewModelScope.launch {

            val current = _state.value

            _state.value = when {

                isRefresh && current is BlogState.Success ->
                    current.copy(isRefreshing = true)

                else -> BlogState.Loading
            }

            when (val result = getBlogsUseCase()) {

                is Result.Success ->
                    _state.value = BlogState.Success(
                        blogs = result.data,
                        isRefreshing = false,
                    )

                is Result.Error -> {

                    val message = result.failure.message

                    if (current is BlogState.Success) {
                        _state.value = current.copy(
                            isRefreshing = false
                        )
                        _effect.send(
                            BlogEffect.ShowSnackbar(message)
                        )
                    } else {
                        _state.value = BlogState.Failure(message)
                    }
                }
            }
        }
    }
}
