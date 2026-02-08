package edu.fullsail.anchor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import edu.fullsail.anchor.engagement.UserEngagementStats

class TaskViewModel : ViewModel() {

    // Use StateFlow to hold the task list, which allows the UI to observe changes.
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks = _tasks.asStateFlow()

    init {
        // Added some sample data for demonstration purposes, so we can have something to show in the meeting.
        _tasks.value = listOf(
            Task(title = "Design new app icon", dueDateMillis = System.currentTimeMillis() + 172800000, priority = "High", timeframe = "Weekly"),
            Task(title = "Write team status report", dueDateMillis = System.currentTimeMillis() + 86400000, priority = "High", timeframe = "Daily"),
            Task(title = "Review PR from Dustin", dueDateMillis = null, priority = "High", timeframe = "Daily"),
            Task(title = "Plan team offsite", dueDateMillis = System.currentTimeMillis() + 259200000, priority = "High", timeframe = "Monthly"),
            Task(title = "Fix bug #123", dueDateMillis = System.currentTimeMillis() + 3600000, priority = "Medium", timeframe = "Daily", isCompleted = true),
            Task(title = "Respond to user feedback", dueDateMillis = System.currentTimeMillis() + 604800000, priority = "Medium", timeframe = "Weekly"),
            Task(title = "Update documentation", dueDateMillis = null, priority = "Low", timeframe = "Monthly"),
            Task(title = "Research new libraries", dueDateMillis = System.currentTimeMillis() + 1209600000, priority = "Low", timeframe = "Yearly"),
            Task(title = "Refactor login screen", dueDateMillis = null, priority = "Low", timeframe = "Monthly")
        )
    }
    /*
    Required Badge System: Builds UserEngagementStats from current tasks
    Used by the badge evaluation pipeline when tasks are completed.
    Do not remove or change without updating badge logic.
     */
    fun buildEngagementStats(): UserEngagementStats {
        val completedTotal = _tasks.value.count { it.isCompleted }
        return UserEngagementStats(
            completedTasksTotal = completedTotal,
            scheduledToday = 0,
            completedToday = 0,
            streakDays = 0
        )
    }

    fun toggleTaskCompletion(taskId: String) {
        _tasks.update { currentTasks ->
            currentTasks.map { task ->
                if (task.id == taskId) {
                    task.copy(isCompleted = !task.isCompleted)
                } else {
                    task
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        _tasks.update { currentTasks ->
            currentTasks.filterNot { it.id == taskId }
        }
    }

    fun updatePriority(taskId: String, newPriority: String) {
        _tasks.update { currentTasks ->
            currentTasks.map { task ->
                if (task.id == taskId) {
                    task.copy(priority = newPriority)
                } else {
                    task
                }
            }
        }
    }

    fun getTaskById(taskId: String): Task? {
        return _tasks.value.find { it.id == taskId }
    }

    fun addTask(title: String, dueDateMillis: Long?, priority: String, timeframe: String): Boolean {
        if (title.isBlank()) {
            return false
        }
        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            dueDateMillis = dueDateMillis,
            priority = priority,
            timeframe = timeframe,
            isCompleted = false
        )
        _tasks.update { it + newTask }
        return true
    }
    
    fun updateTask(id: String, title: String, dueDateMillis: Long?, priority: String, timeframe: String): Boolean {
        if (title.isBlank()) {
            return false
        }

        val taskIndex = _tasks.value.indexOfFirst { it.id == id }
        if (taskIndex != -1) {
            val updatedTasks = _tasks.value.toMutableList()
            updatedTasks[taskIndex] = updatedTasks[taskIndex].copy(
                title = title,
                dueDateMillis = dueDateMillis,
                priority = priority,
                timeframe = timeframe
            )
            _tasks.value = updatedTasks
        }
        return true
    }
}
