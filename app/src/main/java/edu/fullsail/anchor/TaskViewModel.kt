package edu.fullsail.anchor

import android.content.Context
import java.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID
import edu.fullsail.anchor.engagement.UserEngagementStats
import edu.fullsail.anchor.data.TaskRepository
import edu.fullsail.anchor.notifications.sendTaskCreatedNotification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class TaskViewModel(
    private val repository: TaskRepository,
    private val appContext: Context,
    private val settingsViewModel: SettingsViewModel
) : ViewModel() {

    val tasks = repository.tasks.stateIn(
        scope          = viewModelScope,
        started        = SharingStarted.WhileSubscribed(5000),
        initialValue   = emptyList()
    )

    /*
    Required Badge System: Builds UserEngagementStats from current tasks.
    Used by the badge evaluation pipeline when tasks are completed.
    Adds daily metrics (scheduledToday/completedToday) for Steadfast + Ten-in-a-Day badges.
    Do not remove or change without updating badge logic.
    */
    fun buildEngagementStats(nowMillis: Long = System.currentTimeMillis()): UserEngagementStats {
        val all = tasks.value
        val completedTasksTotal = all.count { it.isCompleted }

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val endOfDay   = startOfDay + 24L * 60L * 60L * 1000L

        fun isDueToday(dueMillis: Long?) = dueMillis != null && dueMillis in startOfDay until endOfDay

        val scheduledTodayTask = all.filter { task ->
            isDueToday(task.dueDateMillis) || (task.dueDateMillis == null && task.timeframe == "Daily")
        }
        return UserEngagementStats(
            completedTasksTotal = completedTasksTotal,
            scheduledToday      = scheduledTodayTask.size,
            completedToday      = scheduledTodayTask.count { it.isCompleted },
            // ADDED FOR STREAK — compute real streak from completedAtMillis timestamps in Room
            streakDays          = computeStreakDays(all, nowMillis)
        )
    }

    /*
    ADDED FOR STREAK CALCULATION
    Counts consecutive days (ending today or yesterday) with at least one completed task.
    Uses completedAtMillis recorded by toggleTaskCompletion().

    Grace period: streak is active if today OR yesterday has a completion,
    so users aren't penalised for completing tasks late in the day.

    Badge system dependency: streakDays feeds into STREAK_3 and EXCEPTIONAL_WEEK badges.
    Do not remove without updating BadgeRuleEngine.
    */
    private fun computeStreakDays(tasks: List<Task>, nowMillis: Long): Int {
        val dayMillis = 24L * 60L * 60L * 1000L

        val completedDays: Set<Long> = tasks
            .filter { it.isCompleted && it.completedAtMillis != null }
            .map { task ->
                val c = Calendar.getInstance().apply { timeInMillis = task.completedAtMillis!! }
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            .toSet()

        if (completedDays.isEmpty()) return 0

        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0);      todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart     = todayCal.timeInMillis
        val yesterdayStart = todayStart - dayMillis

        if (completedDays.max() < yesterdayStart) return 0

        var checkDay = if (completedDays.contains(todayStart)) todayStart else yesterdayStart
        var streak   = 0
        while (completedDays.contains(checkDay)) { streak++; checkDay -= dayMillis }
        return streak
    }

    fun toggleTaskCompletion(taskId: String) {
        val now  = System.currentTimeMillis()
        val task = tasks.value.find { it.id == taskId } ?: return
        val isNowComplete = !task.isCompleted
        val updatedTask = task.copy(
            isCompleted       = isNowComplete,
            // Badge system dependency: records completion timestamps for streak achievements
            completedAtMillis = if (isNowComplete) now else null
        )
        viewModelScope.launch { repository.updateTask(updatedTask) }
    }

    fun deleteTask(taskId: String) {
        val task = tasks.value.find { it.id == taskId } ?: return
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun updatePriority(taskId: String, newPriority: String) {
        val task = tasks.value.find { it.id == taskId } ?: return
        viewModelScope.launch { repository.updateTask(task.copy(priority = newPriority)) }
    }

    fun getTaskById(taskId: String): Task? = tasks.value.find { it.id == taskId }

    /**
     * Creates and persists a new task. Returns false (without saving) if the title is blank.
     * New tasks receive Int.MAX_VALUE as their sortOrder so they always appear at the bottom
     * of the list until the user reorders them.
     *
     * [subtasks] is optional — passing a pre-built list from CreateTaskScreen saves the user
     * from having to expand the task card and add subtasks as a second step after creation.
     */
    fun addTask(
        title: String,
        dueDateMillis: Long?,
        priority: String,
        timeframe: String,
        subtasks: List<Subtask> = emptyList()   // ADDED FOR CREATE-SCREEN SUBTASKS
    ): Boolean {
        if (title.isBlank()) return false
        // New tasks get Int.MAX_VALUE so they appear at the bottom of the list.
        // Real indices (0, 1, 2, ...) are assigned after the first drag-and-drop reorder.
        val newTask = Task(
            id            = UUID.randomUUID().toString(),
            title         = title,
            dueDateMillis = dueDateMillis,
            priority      = priority,
            timeframe     = timeframe,
            isCompleted   = false,
            sortOrder     = Int.MAX_VALUE,
            subtasks      = subtasks            // ADDED FOR CREATE-SCREEN SUBTASKS
        )
        viewModelScope.launch {
            repository.insertTask(newTask)
            // Send a notification when a task is created, if the user has notifications enabled.
            // Uses appContext (not Activity context) so it's safe to call from a coroutine.
            if (settingsViewModel.settings.value.notificationsEnabled) {
                sendTaskCreatedNotification(appContext, newTask.title)
            }
        }
        return true
    }

    /**
     * Updates the editable fields of an existing task. Returns false if the title is blank.
     * Preserves isCompleted, completedAtMillis, and sortOrder from the existing record.
     *
     * [subtasks] replaces the task's entire subtask list. Passing the list from
     * CreateTaskScreen means the user can add, remove, or reorder subtasks while editing
     * without needing to open the inline checklist on the task card afterward.
     */
    fun updateTask(
        id: String,
        title: String,
        dueDateMillis: Long?,
        priority: String,
        timeframe: String,
        subtasks: List<Subtask>? = null         // ADDED FOR CREATE-SCREEN SUBTASKS
    ): Boolean {
        if (title.isBlank()) return false
        val existing = tasks.value.find { it.id == id } ?: return false
        viewModelScope.launch {
            repository.updateTask(
                existing.copy(
                    title         = title,
                    dueDateMillis = dueDateMillis,
                    priority      = priority,
                    timeframe     = timeframe,
                    // If the caller passes a subtask list, use it; otherwise keep existing subtasks.
                    // This preserves inline-checklist changes made outside the edit screen.
                    subtasks      = subtasks ?: existing.subtasks   // ADDED FOR CREATE-SCREEN SUBTASKS
                )
            )
        }
        return true
    }


    // ADDED FOR DRAG & DROP REORDERING
    /**
     * Persists the user's new task order after a drag-and-drop gesture completes.
     * [reorderedActiveTasks] is the full non-completed task list in the desired order.
     * Each task receives a sequential sortOrder (0, 1, 2, ...) written to Room.
     */
    fun reorderTasks(reorderedActiveTasks: List<Task>) {
        viewModelScope.launch { repository.reorderTasks(reorderedActiveTasks) }
    }

    // ADDED FOR SUBTASKS
    /** Appends a new subtask to a task. Blank titles are silently ignored. */
    fun addSubtask(taskId: String, subtaskTitle: String) {
        if (subtaskTitle.isBlank()) return
        val task = tasks.value.find { it.id == taskId } ?: return
        viewModelScope.launch {
            repository.updateTask(
                task.copy(subtasks = task.subtasks + Subtask(title = subtaskTitle.trim()))
            )
        }
    }

    /** Flips isDone on a single subtask within a parent task. */
    fun toggleSubtask(taskId: String, subtaskId: String) {
        val task = tasks.value.find { it.id == taskId } ?: return
        viewModelScope.launch {
            repository.updateTask(
                task.copy(
                    subtasks = task.subtasks.map { s ->
                        if (s.id == subtaskId) s.copy(isDone = !s.isDone) else s
                    }
                )
            )
        }
    }

    /** Removes a subtask from a parent task by subtask ID. */
    fun deleteSubtask(taskId: String, subtaskId: String) {
        val task = tasks.value.find { it.id == taskId } ?: return
        viewModelScope.launch {
            repository.updateTask(
                task.copy(subtasks = task.subtasks.filter { it.id != subtaskId })
            )
        }
    }
}

/**
 * Factory required to construct TaskViewModel with a TaskRepository parameter.
 * The default ViewModelProvider cannot inject constructor arguments.
 */
class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val appContext: Context,
    private val settingsViewModel: SettingsViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, appContext, settingsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}