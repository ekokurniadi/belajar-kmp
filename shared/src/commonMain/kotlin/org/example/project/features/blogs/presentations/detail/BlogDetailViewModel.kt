package org.example.project.features.blogs.presentations.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.usecases.GetBlogByIdUseCase

class BlogDetailViewModel(
    private val getBlogByIdUseCase: GetBlogByIdUseCase,
) : ViewModel() {

    private val _state =
        MutableStateFlow<BlogDetailState>(BlogDetailState.Initial)

    val state = _state.asStateFlow()

    private val _effect = Channel<BlogDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentBlogId: Int? = null

    fun onIntent(intent: BlogDetailIntent) {

        when (intent) {

            is BlogDetailIntent.LoadBlog -> {
                currentBlogId = intent.id
                loadBlog(intent.id)
            }

            BlogDetailIntent.Retry -> {
                currentBlogId?.let { loadBlog(it) }
            }
        }
    }

    private fun loadBlog(id: Int) {

        viewModelScope.launch {

            _state.value = BlogDetailState.Loading

            when (val result = getBlogByIdUseCase(id)) {

                is Result.Success ->
                    _state.value = BlogDetailState.Success(
                        blog = result.data
                    )

                is Result.Error -> {

                    val message = result.failure.message

                    _state.value = BlogDetailState.Failure(message)

                    _effect.send(
                        BlogDetailEffect.ShowSnackbar(message)
                    )
                }
            }
        }
    }
}
