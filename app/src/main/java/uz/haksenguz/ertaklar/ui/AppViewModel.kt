package uz.haksenguz.ertaklar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import uz.haksenguz.ertaklar.core.i18n.KoreanMenus
import uz.haksenguz.ertaklar.data.content.ContentRepository
import uz.haksenguz.ertaklar.data.repository.SettingsRepository
import uz.haksenguz.ertaklar.ui.text.Display

/** App-wide state shared by every screen: how text is displayed. */
@HiltViewModel
class AppViewModel @Inject constructor(
    settings: SettingsRepository,
    content: ContentRepository
) : ViewModel() {

    private val menus = flow<KoreanMenus?> { emit(content.koreanMenus()) }.onStart { emit(null) }

    val display: StateFlow<Display> = combine(settings.settings, menus) { s, m -> Display(s.script, s.lang, m) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Display())
}
