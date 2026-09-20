package fr.corentin.biblioscan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.corentin.biblioscan.ui.detail.BookDetailScreen
import fr.corentin.biblioscan.ui.library.LibraryScreen
import fr.corentin.biblioscan.ui.scan.ScanScreen

private object Routes {
    const val SCAN = "scan"
    const val LIBRARY = "library"
    const val DETAIL = "detail/{isbn}"
    fun detail(isbn: String) = "detail/$isbn"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(onOpenLibrary = { navController.navigate(Routes.LIBRARY) })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onOpenBook = { isbn -> navController.navigate(Routes.detail(isbn)) }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("isbn") { type = NavType.StringType })
        ) { backStackEntry ->
            val isbn = backStackEntry.arguments?.getString("isbn").orEmpty()
            BookDetailScreen(isbn = isbn, onBack = { navController.popBackStack() })
        }
    }
}
