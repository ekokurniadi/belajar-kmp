package org.example.project.features.blogs.presentations.detail

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
import org.example.project.features.blogs.domain.usecases.GetBlogByIdUseCase
import org.example.project.features.blogs.fakes.FakeBlogRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BlogDetailViewModelTest {

    private lateinit var fakeRepository: FakeBlogRepository
    private lateinit var useCase: GetBlogByIdUseCase
    private lateinit var viewModel: BlogDetailViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        fakeRepository = FakeBlogRepository()
        useCase = GetBlogByIdUseCase(fakeRepository)
        viewModel = BlogDetailViewModel(useCase)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleBlog(id: Int = 1) = BlogModel(
        userId = 1,
        id = id,
        title = "Detail $id",
        body = "Body $id",
    )

    // ============================================================
    //  Initial State (BlogDetailViewModel tidak auto-load)
    // ============================================================

    @Test
    fun `initial state is Initial before any intent`() = runTest {

        // ViewModel baru dibuat, belum ada intent
        // (Beda dengan BlogViewModel yang auto-load di init)
        assertEquals(BlogDetailState.Initial, viewModel.state.value)
    }

    // ============================================================
    //  LoadBlog Intent
    // ============================================================

    @Test
    fun `LoadBlog transitions to Loading then Success`() = runTest {

        // Given
        val blog = sampleBlog(id = 42)
        fakeRepository.blogByIdResult = Result.Success(blog)

        viewModel.state.test {

            // 1. Initial
            assertEquals(BlogDetailState.Initial, awaitItem())

            // 2. Trigger LoadBlog
            viewModel.onIntent(BlogDetailIntent.LoadBlog(42))

            // 3. Loading
            assertEquals(BlogDetailState.Loading, awaitItem())

            // 4. Success dengan blog yang benar
            val success = awaitItem()
            assertIs<BlogDetailState.Success>(success)
            assertEquals(blog, success.blog)

            cancelAndConsumeRemainingEvents()
        }

        // Verifikasi: id yang diminta diteruskan ke repository
        assertEquals(42, fakeRepository.lastRequestedId)
    }

    @Test
    fun `LoadBlog transitions to Loading then Failure on error`() = runTest {

        // Given
        fakeRepository.blogByIdResult = Result.Error(
            Failure.NetworkFailure("Blog not found")
        )

        viewModel.state.test {

            assertEquals(BlogDetailState.Initial, awaitItem())

            viewModel.onIntent(BlogDetailIntent.LoadBlog(999))

            assertEquals(BlogDetailState.Loading, awaitItem())

            val failure = awaitItem()
            assertIs<BlogDetailState.Failure>(failure)
            assertEquals("Blog not found", failure.message)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Failure load emits ShowSnackbar effect`() = runTest {

        // Given
        fakeRepository.blogByIdResult = Result.Error(
            Failure.NetworkFailure("API error")
        )

        viewModel.effect.test {

            viewModel.onIntent(BlogDetailIntent.LoadBlog(1))
            runCurrent()

            val effect = awaitItem()
            assertIs<BlogDetailEffect.ShowSnackbar>(effect)
            assertEquals("API error", effect.message)
        }
    }

    // ============================================================
    //  Retry Intent
    // ============================================================

    @Test
    fun `Retry uses last loaded blogId`() = runTest {

        // Given - load blog 5 dulu (gagal)
        fakeRepository.blogByIdResult = Result.Error(
            Failure.NetworkFailure("Network down")
        )
        viewModel.onIntent(BlogDetailIntent.LoadBlog(5))
        runCurrent()

        // Now setup success response
        val blog = sampleBlog(id = 5)
        fakeRepository.blogByIdResult = Result.Success(blog)

        // When - Retry
        viewModel.onIntent(BlogDetailIntent.Retry)
        runCurrent()

        // Then - blog 5 yang di-load ulang (bukan id lain)
        assertEquals(5, fakeRepository.lastRequestedId)
        assertEquals(2, fakeRepository.getBlogByIdCallCount)

        // State sudah Success
        val state = viewModel.state.value
        assertIs<BlogDetailState.Success>(state)
        assertEquals(blog, state.blog)
    }

    @Test
    fun `Retry does nothing when LoadBlog never called`() = runTest {

        // Given - belum pernah load
        // currentBlogId masih null

        // When - retry tanpa pernah load
        viewModel.onIntent(BlogDetailIntent.Retry)
        runCurrent()

        // Then - repository tidak terpanggil
        assertEquals(0, fakeRepository.getBlogByIdCallCount)

        // State tetap Initial
        assertEquals(BlogDetailState.Initial, viewModel.state.value)
    }
}
