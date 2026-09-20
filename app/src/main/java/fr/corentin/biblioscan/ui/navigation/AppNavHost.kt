package fr.corentin.biblioscan.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun AppNavHost(sharedLibraryUri: Uri? = null, onSharedLibraryConsumed: () -> Unit = {}) {
    val navController = rememberNavController()

    // Opening a shared library file always surfaces the library screen so the
    // imported (or refreshed) library is immediately visible.
    LaunchedEffect(sharedLibraryUri) {
        if (sharedLibraryUri != null) {
            navController.navigate(Routes.LIBRARY) { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(onOpenLibrary = { navController.navigate(Routes.LIBRARY) })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onOpenBook = { isbn -> navController.navigate(Routes.detail(isbn)) },
                pendingSharedLibraryUri = sharedLibraryUri,
                onSharedLibraryConsumed = onSharedLibraryConsumed
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
