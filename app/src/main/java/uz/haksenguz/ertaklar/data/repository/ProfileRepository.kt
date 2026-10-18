package uz.haksenguz.ertaklar.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.haksenguz.ertaklar.data.local.SettingsStore
import uz.haksenguz.ertaklar.data.local.dao.ProfileDao
import uz.haksenguz.ertaklar.data.local.entities.ProfileEntity
import uz.haksenguz.ertaklar.data.models.Profile

/** The children on this device and which one is reading. */
@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val settings: SettingsStore
) {
    private val mutex = Mutex()

    val profiles: Flow<List<Profile>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    /** The active child; null only until [ensureActive] has run once. */
    val active: Flow<Profile?> = combine(profiles, settings.settings) { list, s ->
        list.firstOrNull { it.id == s.activeProfile } ?: list.firstOrNull()
    }

    /** There is always a child: the first run makes "Bolajon". */
    suspend fun ensureActive() {
        mutex.withLock {
            val all = dao.all()
            val activeId = settings.settings.first().activeProfile
            when {
                all.isEmpty() -> addLocked(Profile.DEFAULT_NAME, null, null)
                all.none { it.id == activeId } -> settings.setActiveProfile(all.first().id)
            }
        }
    }

    suspend fun use(id: String) = settings.setActiveProfile(id)

    suspend fun add(name: String, avatar: String?, age: Int?): String = mutex.withLock { addLocked(name, avatar, age) }

    private suspend fun addLocked(name: String, avatar: String?, age: Int?): String {
        val taken = dao.all().map { it.avatar }.toSet()
        val id = "p" + System.currentTimeMillis().toString(36) + Random.nextInt(1_679_616).toString(36)
        dao.insert(
            ProfileEntity(
                id = id,
                name = name.trim().ifEmpty { Profile.DEFAULT_NAME },
                avatar = avatar ?: Profile.AVATARS.firstOrNull { it !in taken } ?: Profile.AVATARS.first(),
                age = Profile.cleanAge(age),
                created = System.currentTimeMillis()
            )
        )
        settings.setActiveProfile(id)
        return id
    }

    suspend fun addPoints(id: String, n: Int) = dao.addPoints(id, n)

    suspend fun setLastBook(id: String, book: String?) = dao.setLastBook(id, book)
}

private fun ProfileEntity.toModel() = Profile(id, name, avatar, age, points, lastBook, created)
