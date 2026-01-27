package edu.fullsail.anchor

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        // FIX: Use .add() instead of reassignment
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

        // FIX: Find the index and update the element directly
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

    // --- Validation Logic (unchanged) ---
    private fun validateInput(title: String, dayStr: String, monthStr: String, yearStr: String): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult.Error("Title cannot be empty.")
        }

        if (dayStr.isBlank() && monthStr.isBlank() && yearStr.isBlank()) {
            return ValidationResult.Success
        }

        val year = yearStr.toIntOrNull()
        val month = monthStr.toIntOrNull()
        val day = dayStr.toIntOrNull()

        if (year == null || month == null || day == null) {
            return ValidationResult.Error("Date fields must be numbers or all be empty.")
        }

        if (month < 1 || month > 12) {
            return ValidationResult.Error("Month must be between 1 and 12.")
        }

        try {
            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            return ValidationResult.Error("The day is not valid for the selected month and year.")
        }

        return ValidationResult.Success
    }
}