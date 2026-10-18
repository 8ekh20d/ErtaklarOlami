package uz.haksenguz.ertaklar.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The grown-ups gate before the recording studio: once a grown-up has solved
 * its sum, it stays open for a while (for as long as the app keeps running).
 */
@Singleton
class GateRepository @Inject constructor() {
    private var openUntil = 0L

    val isOpen: Boolean get() = System.currentTimeMillis() < openUntil

    fun open() {
        openUntil = System.currentTimeMillis() + MINUTES * 60_000L
    }

    private companion object {
        const val MINUTES = 15
    }
}
