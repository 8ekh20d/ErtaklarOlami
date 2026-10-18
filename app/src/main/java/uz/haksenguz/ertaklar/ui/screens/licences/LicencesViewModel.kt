package uz.haksenguz.ertaklar.ui.screens.licences

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One licence: what it covers, and its full text. */
data class Licence(val title: String, val about: String, val text: String)

/** The licences of the fonts, emoji, icon and map in the app (assets/licenses). */
@HiltViewModel
class LicencesViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {

    private val _licences = MutableStateFlow<List<Licence>>(emptyList())
    val licences: StateFlow<List<Licence>> = _licences.asStateFlow()

    init {
        viewModelScope.launch {
            _licences.value = withContext(Dispatchers.IO) {
                val files = context.assets.list(DIR).orEmpty().sorted()
                // The ones named below first, in this order; any others after.
                val known = KNOWN.keys.filter { it in files }
                (known + files.filter { it !in KNOWN }).map { file ->
                    val text = context.assets.open("$DIR/$file").bufferedReader().use { it.readText() }
                    val (title, about) = KNOWN[file] ?: (file.substringBeforeLast('.') to "")
                    Licence(title, about, text)
                }
            }
        }
    }

    private companion object {
        const val DIR = "licenses"

        /** File -> (title, what it covers, in Uzbek). */
        val KNOWN = linkedMapOf(
            "twemoji-CC-BY-4.0.txt" to ("Twemoji — CC BY 4.0" to "Rasmlardagi emoji belgilari"),
            "fontawesome-CC-BY-4.0.txt" to ("Font Awesome — CC BY 4.0" to "Ilova belgisidagi kitob"),
            "map-data.txt" to ("Natural Earth · datamaps · world-atlas" to "Pasportdagi xarita"),
            "fredoka-OFL.txt" to ("Fredoka — SIL OFL 1.1" to "Sarlavhalar shrifti"),
            "nunito-OFL.txt" to ("Nunito — SIL OFL 1.1" to "Matn shrifti"),
            "gowun-dodum-OFL.txt" to ("Gowun Dodum — SIL OFL 1.1" to "Koreyscha matn shrifti")
        )
    }
}
