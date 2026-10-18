package uz.haksenguz.ertaklar.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.haksenguz.ertaklar.data.models.Profile
import uz.haksenguz.ertaklar.data.repository.ProfileRepository

/** The profile form: a new child ([id] null) or one being changed. */
data class ProfileForm(
    val id: String?,
    val name: String = "",
    val avatar: String = Profile.AVATARS.first(),
    val age: Int? = null,
    val canDelete: Boolean = false,
    val confirmDelete: Boolean = false
)

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeId: String? = null,
    /** Set while adding or editing a child. */
    val form: ProfileForm? = null
)

/** "Kim o'qiyapti?": choose, add, change or remove a child. */
@HiltViewModel
class ProfilesViewModel @Inject constructor(private val repo: ProfileRepository) : ViewModel() {

    private val form = MutableStateFlow<ProfileForm?>(null)
    private val close = Channel<Unit>(Channel.CONFLATED)

    /** The sheet should close (a child was chosen or added). */
    val closeEvents: Flow<Unit> = close.receiveAsFlow()

    val state: StateFlow<ProfilesUiState> = combine(repo.profiles, repo.active, form) { list, active, f ->
        ProfilesUiState(list, active?.id, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun use(id: String) {
        viewModelScope.launch {
            repo.use(id)
            close.send(Unit)
        }
    }

    /** Opens the form for child [id], or for a new child. */
    fun edit(id: String?) {
        val list = state.value.profiles
        val p = list.firstOrNull { it.id == id }
        val taken = list.map { it.avatar }.toSet()
        form.value = if (p != null) {
            ProfileForm(p.id, p.name, p.avatar, p.age, canDelete = list.size >= 2)
        } else {
            ProfileForm(null, avatar = Profile.AVATARS.firstOrNull { it !in taken } ?: Profile.AVATARS.first())
        }
    }

    fun setName(name: String) = form.update { it?.copy(name = name.take(20)) }

    fun setAvatar(avatar: String) = form.update { it?.copy(avatar = avatar) }

    /** Tapping the chosen age again clears it. */
    fun toggleAge(age: Int) = form.update { it?.copy(age = if (it.age == age) null else age) }

    fun cancel() {
        form.value = null
    }

    fun save() {
        val f = form.value ?: return
        if (f.name.isBlank()) return
        viewModelScope.launch {
            if (f.id == null) {
                repo.add(f.name, f.avatar, f.age)
                form.value = null
                close.send(Unit)
            } else {
                repo.update(f.id, f.name, f.avatar, f.age)
                form.value = null
            }
        }
    }

    fun askDelete(ask: Boolean) = form.update { it?.copy(confirmDelete = ask) }

    fun delete() {
        val id = form.value?.id ?: return
        viewModelScope.launch {
            repo.delete(id)
            form.value = null
        }
    }
}
