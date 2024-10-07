package com.sihiver.aiotunneling.util

import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import com.google.firebase.dataconnect.LogLevel

object LogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun addLog(message: String, level: LogLevel = LogLevel.NONE) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedMessage = "[$timestamp] [${level.name}] $message"
        _logs.value += formattedMessage
        println(formattedMessage) // For debugging
    }

    fun getLogs(): List<String> = _logs.value

    fun clearLogs() {
        _logs.value = emptyList()
    }
}


