package edu.fullsail.anchor

import android.content.Context
import java.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID
import edu.fullsail.anchor.engagement.UserEngagementStats
// ADDED FOR PERSISTENCE
import edu.fullsail.anchor.data.TaskRepository
// ADDED FOR PERSISTENCE
import kotlinx.coroutines.flow.SharingStarted
// ADDED FOR PERSISTENCE
import kotlinx.coroutines.flow.stateIn
import edu.fullsail.anchor.notifications.sendTaskCreatedNotification

// MODIFIED FOR PERSISTENCE
// Constructor now accepts a TaskRepository instead of holding data in-memory.
// The factory at the bottom of this file is required because ViewModel constructors
// with parameters cannot be created by the default ViewModelProvider.
class TaskViewModel(
    // ADDED FOR PERSISTENCE — injected repository replaces the in-memory list
    private val repository: TaskRepository, private val appContext: Context, private val settingsViewModel: SettingsViewModel
) : ViewModel() {

    // MODIFIED FOR PERSISTENCE
    // Previously: private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    // Now: tasks comes from the repository's Flow, converted to StateFlow so the
    // existing UI code that calls taskViewModel.tasks.collectAsState() works unchanged.
    val tasks = repository.tasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // REMOVED FOR PERSISTENCE — the init block with sample data is no longer needed
    // because data persists in the Room database. On first launch the database will
    // be empty. If you need seed data for testing, insert it via a one-time migration
    // or a debug utility rather than re-seeding every launch.
    // The original sample task list is preserved below as a comment for reference:
    //
    // init {
    //     _tasks.value = listOf(
    //         Task(title = "Design new app icon", dueDateMillis = ..., priority = "High", timeframe = "Weekly"),
    //         Task(title = "Write team status report", ...),
    //         ... etc
    //     )
    // }

    /*
    Required Badge System: Builds UserEngagementStats from current tasks
    Used by the badge evaluation pipeline when tasks are completed.
    Adds daily metrics (scheduledToday/completedToday) for Steadfast + Ten-in-a-Day badges
    Do not remove or change without updating badge logic.
     */
    fun buildEngagementStats(nowMillis: Long = System.currentTimeMillis()): UserEngagementStats {
        // MODIFIED FOR PERSISTENCE — reads from tasks.value (StateFlow) instead of _tasks.value
        // The StateFlow is now backed by Room, but the value snapshot works identically.
        val all = tasks.value

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

        // MODIFIED FOR PERSISTENCE — find the task in the current snapshot, then persist via repository
        val task = tasks.value.find { it.id == taskId } ?: return
        val newCompletedState = !task.isCompleted
        val updatedTask = task.copy(
            isCompleted = newCompletedState,
            /*
            Record completion time for streak + daily badge logic
             Badge system dependency: records completion timestamps for streak-based achievements
             */
            completedAtMillis = if (newCompletedState) now else null
        )
        // ADDED FOR PERSISTENCE — persist the toggled state to Room
        viewModelScope.launch {
            repository.updateTask(updatedTask)
        }
    }

    fun deleteTask(taskId: String) {
        // MODIFIED FOR PERSISTENCE — find the task then delete via repository instead of filtering the in-memory list
        val task = tasks.value.find { it.id == taskId } ?: return
        // ADDED FOR PERSISTENCE — delete from Room on a background coroutine
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun updatePriority(taskId: String, newPriority: String) {
        // MODIFIED FOR PERSISTENCE — find the task, copy with new priority, persist to Room
        val task = tasks.value.find { it.id == taskId } ?: return
        val updatedTask = task.copy(priority = newPriority)
        // ADDED FOR PERSISTENCE — write updated priority to Room
        viewModelScope.launch {
            repository.updateTask(updatedTask)
        }
    }

    fun getTaskById(taskId: String): Task? {
        // MODIFIED FOR PERSISTENCE — still reads from the StateFlow snapshot; behaviour unchanged
        return tasks.value.find { it.id == taskId }
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
        // MODIFIED FOR PERSISTENCE — insert into Room instead of updating in-memory list
        // ADDED FOR PERSISTENCE
        viewModelScope.launch {
            repository.insertTask(newTask)

            if (settingsViewModel.settings.value.notificationsEnabled) {
                sendTaskCreatedNotification(appContext, newTask.title)
            }
        }
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

        // MODIFIED FOR PERSISTENCE — find existing task to preserve fields not being edited
        val existingTask = tasks.value.find { it.id == id } ?: return false
        val updatedTask = existingTask.copy(
            title = title,
            dueDateMillis = dueDateMillis,
            priority = priority,
            timeframe = timeframe
        )
        // ADDED FOR PERSISTENCE — persist the edit to Room
        viewModelScope.launch {
            repository.updateTask(updatedTask)
        }
        return true
    }
}

// ADDED FOR PERSISTENCE
/**
 * Factory required to construct TaskViewModel with a TaskRepository parameter.
 * The default ViewModelProvider cannot inject constructor arguments, so this
 * factory is passed to viewModel() in MainActivity / AppNavigation.
 *
 * Usage in a Composable:
 *   val factory = TaskViewModelFactory(TaskRepository(AnchorDatabase.getInstance(context).taskDao()))
 *   val taskViewModel: TaskViewModel = viewModel(factory = factory)
 */
class TaskViewModelFactory(
    // ADDED FOR PERSISTENCE
    private val repository: TaskRepository, private val appContext: Context, private val settingsViewModel: SettingsViewModel
) : ViewModelProvider.Factory {
    // ADDED FOR PERSISTENCE
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository, appContext, settingsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}