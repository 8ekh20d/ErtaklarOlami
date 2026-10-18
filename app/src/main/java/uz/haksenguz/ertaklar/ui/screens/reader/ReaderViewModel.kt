package uz.haksenguz.ertaklar.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.models.Book
import uz.haksenguz.ertaklar.navigation.ReaderRoute

data class ReaderUiState(val book: Book? = null)

/** Phase 1 stand-in: shows which book was opened. The reader itself comes in phase 2. */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository
) : ViewModel() {
    private val route = savedState.toRoute<ReaderRoute>()
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = ReaderUiState(content.library()[route.key]) }
    }
}
