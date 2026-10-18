package uz.haksenguz.ertaklar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.haksenguz.ertaklar.ui.screens.library.LibraryScreen
import uz.haksenguz.ertaklar.ui.screens.quiz.QuizScreen
import uz.haksenguz.ertaklar.ui.screens.reader.ReaderScreen
import uz.haksenguz.ertaklar.ui.screens.studio.StudioScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(onOpenBook = { key, resume -> nav.navigate(ReaderRoute(key, resume)) })
        }
        composable<ReaderRoute> {
            ReaderScreen(
                onBack = { nav.popBackStack() },
                // A twin tale opens in place of the one just finished.
                onOpenBook = { key -> nav.navigate(ReaderRoute(key)) { popUpTo<ReaderRoute> { inclusive = true } } },
                onOpenQuiz = { key -> nav.navigate(QuizRoute(key)) },
                onOpenStudio = { key, page -> nav.navigate(StudioRoute(key, page)) }
            )
        }
        composable<StudioRoute> {
            StudioScreen(onBack = { nav.popBackStack() })
        }
        composable<QuizRoute> {
            QuizScreen(
                onBack = { nav.popBackStack() },
                // After a right answer: back to the library.
                onDone = { nav.popBackStack<LibraryRoute>(inclusive = false) }
            )
        }
    }
}
