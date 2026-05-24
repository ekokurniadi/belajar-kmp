package org.example.project.features.blogs.domain.usecases

import kotlinx.coroutines.test.runTest
import org.example.project.core.util.Failure
import org.example.project.core.util.Result
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.fakes.FakeBlogRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetBlogsUseCaseTest {

    private lateinit var fakeRepository: FakeBlogRepository
    private lateinit var useCase: GetBlogsUseCase

    @BeforeTest
    fun setup() {
        fakeRepository = FakeBlogRepository()
        useCase = GetBlogsUseCase(fakeRepository)
    }

    @Test
    fun `invoke returns Success from repository`() = runTest {

        // Given
        val expectedBlogs = listOf(
            BlogModel(userId = 1, id = 1, title = "Test", body = "Body"),
        )
        fakeRepository.blogsResult = Result.Success(expectedBlogs)

        // When
        val result = useCase()

        // Then
        assertIs<Result.Success<*>>(result)
        assertEquals(expectedBlogs, result.data)

        // Verifikasi: repository dipanggil 1x
        assertEquals(1, fakeRepository.getBlogsCallCount)
    }

    @Test
    fun `invoke returns Error from repository`() = runTest {

        // Given
        fakeRepository.blogsResult = Result.Error(
            Failure.NetworkFailure("Connection failed")
        )

        // When
        val result = useCase()

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Connection failed", result.failure.message)
    }
}
