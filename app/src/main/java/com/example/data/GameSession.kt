package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val avgTemp: Float,
    val avgPing: Int,
    val batteryConsumed: Int
)
