package edu.fullsail.anchor

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

class TaskViewModel : ViewModel() {

    val tasks = mutableStateListOf<Task>()

    fun addTask(title: String, dueDateMillis: Long?, priority: String, timeframe: String): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult.Error("Title cannot be empty.")
        }
        val newTask = Task(
            title = title,
            dueDateMillis = dueDateMillis,
            priority = priority,
            timeframe = timeframe
        )
        tasks.add(newTask)
        return ValidationResult.Success
    }

    fun deleteTask(taskId: String) {
        tasks.removeIf { it.id == taskId }
    }

    fun updateTask(id: String, title: String, dueDateMillis: Long?, priority: String, timeframe: String): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult.Error("Title cannot be empty.")
        }

        val taskIndex = tasks.indexOfFirst { it.id == id }
        if (taskIndex != -1) {
            tasks[taskIndex] = tasks[taskIndex].copy(
                title = title,
                dueDateMillis = dueDateMillis,
                priority = priority,
                timeframe = timeframe
            )
        }
        return ValidationResult.Success
    }

    fun toggleTaskCompletion(taskId: String) {
        val taskIndex = tasks.indexOfFirst { it.id == taskId }
        if (taskIndex != -1) {
            val task = tasks[taskIndex]
            tasks[taskIndex] = task.copy(isCompleted = !task.isCompleted)
        }
    }

    fun getTaskById(taskId: String): Task? {
        return tasks.find { it.id == taskId }
    }
}