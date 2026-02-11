package edu.fullsail.anchor

import java.util.Calendar
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
            Task(
                title = "Design new app icon",
                dueDateMillis = System.currentTimeMillis() + 172800000,
                priority = "High",
                timeframe = "Weekly"
            ),
            Task(
                title = "Write team status report",
                dueDateMillis = System.currentTimeMillis() + 86400000,
                priority = "High",
                timeframe = "Daily"
            ),
            Task(
                title = "Review PR from Dustin",
                dueDateMillis = null,
                priority = "High",
                timeframe = "Daily"
            ),
            Task(
                title = "Plan team offsite",
                dueDateMillis = System.currentTimeMillis() + 259200000,
                priority = "High",
                timeframe = "Monthly"
            ),
            Task(
                title = "Fix bug #123",
                dueDateMillis = System.currentTimeMillis() + 3600000,
                priority = "Medium",
                timeframe = "Daily",
                isCompleted = true
            ),
            Task(
                title = "Respond to user feedback",
                dueDateMillis = System.currentTimeMillis() + 604800000,
                priority = "Medium",
                timeframe = "Weekly"
            ),
            Task(
                title = "Update documentation",
                dueDateMillis = null,
                priority = "Low",
                timeframe = "Monthly"
            ),
            Task(
                title = "Research new libraries",
                dueDateMillis = System.currentTimeMillis() + 1209600000,
                priority = "Low",
                timeframe = "Yearly"
            ),
            Task(
                title = "Refactor login screen",
                dueDateMillis = null,
                priority = "Low",
                timeframe = "Monthly"
            )
        )
    }

    /*
    Required Badge System: Builds UserEngagementStats from current tasks
    Used by the badge evaluation pipeline when tasks are completed.
    Adds daily metrics (scheduledToday/completedToday) for Steadfast + Ten-in-a-Day badges
    Do not remove or change without updating badge logic.
     */
    fun buildEngagementStats(nowMillis: Long = System.currentTimeMillis()): UserEngagementStats {
        val all = _tasks.value

        val completedTasksTotal = all.count { it.isCompleted }

        //Compute local start/end of today (inclusive start, exclusive end)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 24L * 60L * 60L * 1000L

        fun isDueToday(dueMillis: Long?): Boolean {
            if(dueMillis == null) return false
            return dueMillis in startOfDay until endOfDay
        }

        //"scheduled today" = due today or (daily task with no date)
        val scheduledTodayTask = all.filter { task ->
            isDueToday(task.dueDateMillis) || (task.dueDateMillis == null && task.timeframe == "Daily")
        }
        return UserEngagementStats(
            completedTasksTotal = completedTasksTotal,
            scheduledToday = scheduledTodayTask.size,
            completedToday = scheduledTodayTask.count { it.isCompleted },
            streakDays = 0 // still need to complete this
        )
    }

    fun toggleTaskCompletion(taskId: String) {
        val now = System.currentTimeMillis()

        _tasks.update { currentTasks ->
            currentTasks.map { task ->
                if (task.id == taskId) {
                    val newCompletedState = !task.isCompleted
                    task.copy(
                        isCompleted = newCompletedState,
                        /*
                        Record completion time for streak + daily badge logic
                         Badge system dependency: records completion timestamps for streak-based achievements
                         */
                        completedAtMillis = if (newCompletedState) now else null
                    )
            } else (
                task
            )
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

    fun updateTask(
        id: String,
        title: String,
        dueDateMillis: Long?,
        priority: String,
        timeframe: String
    ): Boolean {
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
