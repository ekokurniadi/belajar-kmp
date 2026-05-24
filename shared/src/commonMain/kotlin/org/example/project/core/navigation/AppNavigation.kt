package org.example.project.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.example.project.features.blogs.presentations.detail.BlogDetailScreen
import org.example.project.features.blogs.presentations.list.BlogScreen

/**
 * Type-safe routes via @Serializable sealed interface.
 * Lebih type-safe daripada string-based routes.
 */
sealed interface AppRoute {

    @Serializable
    data object BlogList : AppRoute

    @Serializable
    data class BlogDetail(val blogId: Int) : AppRoute
}

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.BlogList,
    ) {

        composable<AppRoute.BlogList> {
            BlogScreen(
                onBlogClick = { blogId ->
                    navController.navigate(
                        AppRoute.BlogDetail(blogId)
                    )
                },
            )
        }

        composable<AppRoute.BlogDetail> { backStackEntry ->

            val route: AppRoute.BlogDetail =
                backStackEntry.toRoute()

            BlogDetailScreen(
                blogId = route.blogId,
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
