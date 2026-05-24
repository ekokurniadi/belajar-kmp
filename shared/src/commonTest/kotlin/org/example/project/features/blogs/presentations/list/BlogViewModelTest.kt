package org.example.project.features.blogs.presentations.list

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.example.project.core.util.Failure
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.domain.usecases.GetBlogsUseCase
import org.example.project.features.blogs.fakes.FakeBlogRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BlogViewModelTest {

    private lateinit var fakeRepository: FakeBlogRepository
    private lateinit var useCase: GetBlogsUseCase

    @BeforeTest
    fun setup() {
        // viewModelScope pakai Dispatchers.Main.
        // Di test, kita perlu override Main supaya
        // coroutine bisa di-control oleh test scheduler.
        Dispatchers.setMain(StandardTestDispatcher())

        fakeRepository = FakeBlogRepository()
        useCase = GetBlogsUseCase(fakeRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper untuk bikin test data
    private fun sampleBlogs() = listOf(
        BlogModel(userId = 1, id = 1, title = "First", body = "Body 1"),
        BlogModel(userId = 2, id = 2, title = "Second", body = "Body 2"),
    )

    // ============================================================
    //  Initial Load (init block)
    // ============================================================

    @Test
    fun `init triggers LoadBlogs and ends in Success`() = runTest {

        // Given
        val blogs = sampleBlogs()
        fakeRepository.blogsResult = Result.Success(blogs)

        // When - construct ViewModel (init { onIntent(LoadBlogs) })
        val viewModel = BlogViewModel(useCase)

        // Let viewModelScope coroutine run
        runCurrent()

        // Then
        val state = viewModel.state.value
        assertIs<BlogState.Success>(state)
        assertEquals(blogs, state.blogs)
        assertFalse(state.isRefreshing)

        // Repository dipanggil 1x dari init
        assertEquals(1, fakeRepository.getBlogsCallCount)
    }

    @Test
    fun `init triggers LoadBlogs and ends in Failure on error`() = runTest {

        // Given
        fakeRepository.blogsResult = Result.Error(
            Failure.NetworkFailure("Connection failed")
        )

        // When
        val viewModel = BlogViewModel(useCase)
        runCurrent()

        // Then
        val state = viewModel.state.value
        assertIs<BlogState.Failure>(state)
        assertEquals("Connection failed", state.message)
    }

    // ============================================================
    //  State Transitions (Turbine)
    // ============================================================

    @Test
    fun `state transitions Initial then Loading then Success on first load`() = runTest {

        // Given
        val blogs = sampleBlogs()
        fakeRepository.blogsResult = Result.Success(blogs)

        // When - create VM (init triggers LoadBlogs)
        val viewModel = BlogViewModel(useCase)

        // Then - observe state transitions via Turbine
        viewModel.state.test {

            // 1. Initial state (StateFlow always emits current value first)
            assertEquals(BlogState.Initial, awaitItem())

            // 2. Loading (set by loadBlogs())
            assertEquals(BlogState.Loading, awaitItem())

            // 3. Success
            val success = awaitItem()
            assertIs<BlogState.Success>(success)
            assertEquals(blogs, success.blogs)

            // Tidak ada emisi lain
            cancelAndConsumeRemainingEvents()
        }
    }

    // ============================================================
    //  Refresh Behavior
    // ============================================================

    @Test
    fun `Refresh from Success keeps blogs and sets isRefreshing`() = runTest {

        // Given - mulai dari Success state
        val initialBlogs = sampleBlogs()
        fakeRepository.blogsResult = Result.Success(initialBlogs)

        val viewModel = BlogViewModel(useCase)
        runCurrent()

        // When - trigger Refresh
        val refreshedBlogs = initialBlogs + BlogModel(
            userId = 3, id = 3, title = "New", body = "Body 3"
        )
        fakeRepository.blogsResult = Result.Success(refreshedBlogs)

        viewModel.state.test {

            // 1. Initial emission = current state = Success(initialBlogs)
            val before = awaitItem()
            assertIs<BlogState.Success>(before)
            assertEquals(initialBlogs, before.blogs)

            // 2. Trigger refresh
            viewModel.onIntent(BlogIntent.Refresh)

            // 3. Selama refresh: tetap Success, tapi isRefreshing=true
            //    Data lama tetap tampil (UX bagus)
            val refreshing = awaitItem()
            assertIs<BlogState.Success>(refreshing)
            assertTrue(refreshing.isRefreshing)
            assertEquals(initialBlogs, refreshing.blogs) // data lama!

            // 4. Setelah selesai: Success dengan data baru
            val finalState = awaitItem()
            assertIs<BlogState.Success>(finalState)
            assertFalse(finalState.isRefreshing)
            assertEquals(refreshedBlogs, finalState.blogs)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Refresh failure keeps Success state and emits Snackbar effect`() = runTest {

        // Given - mulai dari Success state
        val initialBlogs = sampleBlogs()
        fakeRepository.blogsResult = Result.Success(initialBlogs)

        val viewModel = BlogViewModel(useCase)
        runCurrent()

        // When - refresh gagal
        fakeRepository.blogsResult = Result.Error(
            Failure.NetworkFailure("Refresh failed")
        )

        viewModel.effect.test {

            // Trigger refresh
            viewModel.onIntent(BlogIntent.Refresh)
            runCurrent()

            // Then - Snackbar effect terkirim
            val effect = awaitItem()
            assertIs<BlogEffect.ShowSnackbar>(effect)
            assertEquals("Refresh failed", effect.message)
        }

        // State tetap Success dengan data lama (gak transition ke Failure)
        val state = viewModel.state.value
        assertIs<BlogState.Success>(state)
        assertEquals(initialBlogs, state.blogs)
        assertFalse(state.isRefreshing)
    }

    // ============================================================
    //  Navigation Effect
    // ============================================================

    @Test
    fun `BlogClicked emits NavigateToDetail effect with correct id`() = runTest {

        // Given
        fakeRepository.blogsResult = Result.Success(sampleBlogs())
        val viewModel = BlogViewModel(useCase)
        runCurrent()

        // When + Then
        viewModel.effect.test {

            viewModel.onIntent(BlogIntent.BlogClicked(id = 42))

            val effect = awaitItem()
            assertIs<BlogEffect.NavigateToDetail>(effect)
            assertEquals(42, effect.blogId)
        }
    }
}
