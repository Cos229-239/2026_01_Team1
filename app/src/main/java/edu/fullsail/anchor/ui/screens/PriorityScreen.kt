package edu.fullsail.anchor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.fullsail.anchor.Task
import edu.fullsail.anchor.TaskViewModel

@Composable
fun PriorityScreen(
    navController: NavController,
    viewModel: TaskViewModel = viewModel()
) {
    val allTasks by viewModel.tasks.collectAsState()

    // Memoize the filtered lists to avoid re-calculation on every recomposition.
    val (high, medium, low) = remember(allTasks) {
        val incomplete = allTasks.filter { !it.isCompleted }
        val high = incomplete.filter { it.priority == "High" }
        val medium = incomplete.filter { it.priority == "Medium" }
        val low = incomplete.filter { it.priority == "Low" }
        Triple(high, medium, low)
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- FOCUS Section (High Priority) ---
        item {
            val focusCount = high.size
            val additionalCount = (focusCount - 3).coerceAtLeast(0)
            PrioritySectionHeader("⭐ Focus", additionalCount)
        }
        items(high.take(3), key = { task: Task -> task.id }) { task ->
            PriorityTaskRow(
                task = task,
                onToggle = { viewModel.toggleTaskCompletion(task.id) },
                onDelete = { viewModel.deleteTask(task.id) },
                onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                onPriorityChange = { newPriority -> viewModel.updatePriority(task.id, newPriority) }
            )
        }

        // --- ACTIVE Section (Medium Priority) ---
        if (medium.isNotEmpty()) {
            item { PrioritySectionHeader("☑ Active") }
            items(medium, key = { task: Task -> task.id }) { task ->
                PriorityTaskRow(
                    task = task,
                    onToggle = { viewModel.toggleTaskCompletion(task.id) },
                    onDelete = { viewModel.deleteTask(task.id) },
                    onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                    onPriorityChange = { newPriority -> viewModel.updatePriority(task.id, newPriority) }
                )
            }
        }

        // --- LATER/OPTIONAL Section (Low Priority) ---
        if (low.isNotEmpty()) {
            item { PrioritySectionHeader("⏳ Later/Optional") }
            items(low, key = { task: Task -> task.id }) { task ->
                PriorityTaskRow(
                    task = task,
                    onToggle = { viewModel.toggleTaskCompletion(task.id) },
                    onDelete = { viewModel.deleteTask(task.id) },
                    onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                    onPriorityChange = { newPriority -> viewModel.updatePriority(task.id, newPriority) }
                )
            }
        }
    }
}

@Composable
private fun PrioritySectionHeader(title: String, additionalCount: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (additionalCount > 0) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "+$additionalCount additional tasks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityTaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPriorityChange: (String) -> Unit
) {
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (task.dueDate.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Task")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }
            Box {
                IconButton(onClick = { showPriorityMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Change Priority")
                }
                DropdownMenu(
                    expanded = showPriorityMenu,
                    onDismissRequest = { showPriorityMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("High Priority") }, onClick = {
                        onPriorityChange("High")
                        showPriorityMenu = false
                    })
                    DropdownMenuItem(text = { Text("Medium Priority") }, onClick = {
                        onPriorityChange("Medium")
                        showPriorityMenu = false
                    })
                    DropdownMenuItem(text = { Text("Low Priority") }, onClick = {
                        onPriorityChange("Low")
                        showPriorityMenu = false
                    })
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete Task") },
            text = { Text(text = "Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(text = "Edit Task") },
            text = { Text(text = "Are you sure you want to edit this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEdit()
                        showEditDialog = false
                    }
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
