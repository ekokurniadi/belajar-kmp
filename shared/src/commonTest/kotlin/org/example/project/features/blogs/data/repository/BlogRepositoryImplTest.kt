package org.example.project.features.blogs.data.repository

import kotlinx.coroutines.test.runTest
import org.example.project.core.util.Failure
import org.example.project.core.util.Result
import org.example.project.features.blogs.data.dto.BlogDto
import org.example.project.features.blogs.domain.model.BlogModel
import org.example.project.features.blogs.fakes.FakeBlogsApi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlogRepositoryImplTest {

    private lateinit var fakeApi: FakeBlogsApi
    private lateinit var repository: BlogRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeApi = FakeBlogsApi()
        repository = BlogRepositoryImpl(fakeApi)
    }

    // ============================================================
    //  getBlogs()
    // ============================================================

    @Test
    fun `getBlogs returns Success and maps DTO to Domain`() = runTest {

        // Given - API return 2 BlogDto
        fakeApi.blogsResponse = listOf(
            BlogDto(userId = 1, id = 1, title = "First", body = "Body 1"),
            BlogDto(userId = 2, id = 2, title = "Second", body = "Body 2"),
        )

        // When
        val result = repository.getBlogs()

        // Then
        val success = assertIs<Result.Success<List<BlogModel>>>(result)
        assertEquals(2, success.data.size)
        assertEquals("First", success.data[0].title)
        assertEquals(1, success.data[0].id)
        assertEquals("Second", success.data[1].title)
    }

    @Test
    fun `getBlogs returns Error with NetworkFailure when API throws`() = runTest {

        // Given - API simulasi error
        fakeApi.shouldThrowOnGetBlogs = true
        fakeApi.thrownException = RuntimeException("No internet")

        // When
        val result = repository.getBlogs()

        // Then
        assertIs<Result.Error>(result)
        assertIs<Failure.NetworkFailure>(result.failure)
        assertEquals("No internet", result.failure.message)
    }

    @Test
    fun `getBlogs returns Success with empty list when API returns empty`() = runTest {

        // Given - API return list kosong
        fakeApi.blogsResponse = emptyList()

        // When
        val result = repository.getBlogs()

        // Then
        val success = assertIs<Result.Success<List<BlogModel>>>(result)
        assertTrue(success.data.isEmpty())
    }

    // ============================================================
    //  getBlogById(id)
    // ============================================================

    @Test
    fun `getBlogById returns Success and maps DTO to Domain`() = runTest {

        // Given
        fakeApi.blogByIdResponse =
            BlogDto(userId = 5, id = 42, title = "Detail", body = "Body detail")

        // When
        val result = repository.getBlogById(42)

        // Then
        val success = assertIs<Result.Success<BlogModel>>(result)
        val blog = success.data
        assertEquals(42, blog.id)
        assertEquals(5, blog.userId)
        assertEquals("Detail", blog.title)
        assertEquals("Body detail", blog.body)

        // Verifikasi: id yang diminta diteruskan ke API
        assertEquals(42, fakeApi.lastRequestedId)
    }

    @Test
    fun `getBlogById returns Error when API throws`() = runTest {

        // Given
        fakeApi.shouldThrowOnGetBlogById = true
        fakeApi.thrownException = RuntimeException("404 Not Found")

        // When
        val result = repository.getBlogById(999)

        // Then
        assertIs<Result.Error>(result)
        assertIs<Failure.NetworkFailure>(result.failure)
        assertEquals("404 Not Found", result.failure.message)
    }
}
