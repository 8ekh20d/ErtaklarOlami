package uz.haksenguz.ertaklar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.haksenguz.ertaklar.ui.screens.library.LibraryScreen
import uz.haksenguz.ertaklar.ui.screens.reader.ReaderScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(onOpenBook = { key, resume -> nav.navigate(ReaderRoute(key, resume)) })
        }
        composable<ReaderRoute> {
            ReaderScreen(onBack = { nav.popBackStack() })
        }
    }
}
