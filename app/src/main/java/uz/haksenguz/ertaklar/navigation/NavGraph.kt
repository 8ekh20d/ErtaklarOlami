package uz.haksenguz.ertaklar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import uz.haksenguz.ertaklar.data.repository.DictionaryRepository
import uz.haksenguz.ertaklar.ui.screens.coloring.ColoringScreen
import uz.haksenguz.ertaklar.ui.screens.compare.CompareScreen
import uz.haksenguz.ertaklar.ui.screens.dictgames.ListenGameScreen
import uz.haksenguz.ertaklar.ui.screens.dictgames.MatchGameScreen
import uz.haksenguz.ertaklar.ui.screens.dictgames.SpellGameScreen
import uz.haksenguz.ertaklar.ui.screens.dictionary.DictionaryScreen
import uz.haksenguz.ertaklar.ui.screens.library.LibraryScreen
import uz.haksenguz.ertaklar.ui.screens.ordergame.OrderGameScreen
import uz.haksenguz.ertaklar.ui.screens.quiz.QuizScreen
import uz.haksenguz.ertaklar.ui.screens.reader.ReaderScreen
import uz.haksenguz.ertaklar.ui.screens.studio.StudioScreen
import uz.haksenguz.ertaklar.ui.screens.trace.TraceScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                onOpenBook = { key, resume -> nav.navigate(ReaderRoute(key, resume)) },
                onBedtime = { key -> nav.navigate(ReaderRoute(key, bedtime = true)) },
                onDictionary = { nav.navigate(DictionaryRoute) }
            )
        }
        composable<DictionaryRoute> {
            DictionaryScreen(
                onBack = { nav.popBackStack() },
                onOpenWord = { key, page -> nav.navigate(ReaderRoute(key, page = page)) },
                onGame = { game -> nav.navigate(DictGameRoute(game)) }
            )
        }
        composable<DictGameRoute> { entry ->
            val back = { nav.popBackStack() }
            when (entry.toRoute<DictGameRoute>().game) {
                DictionaryRepository.MATCH -> MatchGameScreen(onBack = { back() })
                DictionaryRepository.SPELL -> SpellGameScreen(onBack = { back() })
                else -> ListenGameScreen(onBack = { back() })
            }
        }
        composable<ReaderRoute> {
            ReaderScreen(
                onBack = { nav.popBackStack() },
                // A twin tale opens in place of the one just finished.
                onOpenBook = { key -> nav.navigate(ReaderRoute(key)) { popUpTo<ReaderRoute> { inclusive = true } } },
                onOpenQuiz = { key -> nav.navigate(QuizRoute(key)) },
                onOpenStudio = { key, page -> nav.navigate(StudioRoute(key, page)) },
                onPlay = { route -> nav.navigate(route) }
            )
        }
        composable<ColoringRoute> { ColoringScreen(onBack = { nav.popBackStack() }) }
        composable<OrderRoute> { OrderGameScreen(onBack = { nav.popBackStack() }) }
        composable<CompareRoute> { CompareScreen(onBack = { nav.popBackStack() }) }
        composable<TraceRoute> { TraceScreen(onBack = { nav.popBackStack() }) }
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
