package model

import androidx.navigationevent.NavigationEventDispatcher
import java.time.LocalDate

data class Task(
    val id: Int,
    val title: String,
    val dueDate: LocalDate?,
    val priority: Priority,
    val timeframe: Timeframe,
    val isCompleted: Boolean = false
)