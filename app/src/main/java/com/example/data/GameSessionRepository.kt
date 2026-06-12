package com.example.data

import kotlinx.coroutines.flow.Flow

class GameSessionRepository(private val dao: GameSessionDao) {
    val allSessions: Flow<List<GameSession>> = dao.getAllSessions()

    suspend fun insertSession(session: GameSession) {
        dao.insertSession(session)
    }

    suspend fun deleteSession(session: GameSession) {
        dao.deleteSession(session)
    }
}
