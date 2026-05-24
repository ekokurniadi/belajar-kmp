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

class GetBlogByIdUseCaseTest {

    private lateinit var fakeRepository: FakeBlogRepository
    private lateinit var useCase: GetBlogByIdUseCase

    @BeforeTest
    fun setup() {
        fakeRepository = FakeBlogRepository()
        useCase = GetBlogByIdUseCase(fakeRepository)
    }

    @Test
    fun `invoke passes id to repository and returns Success`() = runTest {

        // Given
        val expectedBlog = BlogModel(
            userId = 1,
            id = 42,
            title = "Detail",
            body = "Body",
        )
        fakeRepository.blogByIdResult = Result.Success(expectedBlog)

        // When
        val result = useCase(42)

        // Then
        assertIs<Result.Success<*>>(result)
        assertEquals(expectedBlog, result.data)

        // Verifikasi: id diteruskan ke repository
        assertEquals(42, fakeRepository.lastRequestedId)
        assertEquals(1, fakeRepository.getBlogByIdCallCount)
    }

    @Test
    fun `invoke returns Error from repository`() = runTest {

        // Given
        fakeRepository.blogByIdResult = Result.Error(
            Failure.NetworkFailure("Not found")
        )

        // When
        val result = useCase(999)

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Not found", result.failure.message)
    }
}
