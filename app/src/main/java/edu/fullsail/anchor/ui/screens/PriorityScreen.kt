package edu.fullsail.anchor.ui.screens

import  androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import edu.fullsail.anchor.Task
import edu.fullsail.anchor.TaskViewModel
import edu.fullsail.anchor.engagement.badges.BadgeRuleEngine
import edu.fullsail.anchor.engagement.badges.BadgesViewModel
import edu.fullsail.anchor.engagement.badges.ConfettiOverlay
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import edu.fullsail.anchor.engagement.badges.Explosion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import edu.fullsail.anchor.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext


@Composable
fun PriorityScreen(
    navController: NavController,
    viewModel: TaskViewModel,
    badgesViewModel: BadgesViewModel,
    settingsViewModel: edu.fullsail.anchor.SettingsViewModel  // Receive settings
) {
    val allTasks by viewModel.tasks.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()  // Observe settings
    val context = LocalContext.current

    //prevents showing the same badge toast more than once
    // adding confetti value
    val explosions = remember { mutableStateListOf<Explosion>() }

    // creating offset for confetti to go to check box
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }


    // Memoize the filtered lists to avoid re-calculation on every recomposition.
    val (high, medium, low) = remember(allTasks) {
        val incomplete = allTasks.filter { !it.isCompleted }
        val high = incomplete.filter { it.priority == "High" }
        val medium = incomplete.filter { it.priority == "Medium" }
        val low = incomplete.filter { it.priority == "Low" }
        Triple(high, medium, low)
    }

    // Calculate displayed high tasks based on limitFocusToThree setting
    val displayedHighTasks = remember(high, settings.limitFocusToThree) {
        if (settings.limitFocusToThree) high.take(3) else high
    }

    // Track expanded state for each section
    // NOTE: Using remember instead of rememberSaveable to prevent serialization crashes
    var isFocusExpanded by remember { mutableStateOf(true) }
    var isActiveExpanded by remember { mutableStateOf(true) }
    var isLaterExpanded by remember { mutableStateOf(true) }

    // adding confetti changes to handle completion logic
    val onTaskComplete: (String, Offset) -> Unit = { taskId, rawPosition ->
        viewModel.toggleTaskCompletion(taskId) // for the confetti logic

        val adjustedPosition = rawPosition - overlayOffset


        // add explosion at a specific position
        explosions.add(Explosion(id = System.nanoTime(), position = adjustedPosition))
    }
    // badges logic
    // making this a lunched effect due to issues popping up
    // this resolved all previous issues.
    LaunchedEffect(allTasks) {
        val stats = viewModel.buildEngagementStats()
        val (updateBadges, newlyUnlocked) = BadgeRuleEngine.evaluate(
            stats = stats,
            existing = badgesViewModel.badges
        )
        badgesViewModel.saveBadges(updateBadges)

        //Toast only for newly unlocked badges
        newlyUnlocked.forEach { badge ->
            Toast.makeText(
                context,
                "Badge unlocked: ${badge.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // confetti wrapping it all in a box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayOffset = coordinates.positionInWindow()
            }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- FOCUS Section (High Priority) ---
            item(key = "header_focus") {
                val focusCount = high.size
                // Only show additional count when limit is ON
                val additionalCount = if (settings.limitFocusToThree && focusCount > 3) {
                    (focusCount - 3).coerceAtLeast(0)
                } else {
                    0
                }
                PrioritySectionHeader(
                    title = "Focus",
                    //Calls the PNG image from the drawable file
                    iconRes = R.drawable.focus,
                    iconSize = 30.dp,
                    additionalCount = additionalCount,
                    isExpanded = isFocusExpanded,
                    onToggle = { isFocusExpanded = !isFocusExpanded }
                )
            }

            if (isFocusExpanded) {
                // Use displayedHighTasks instead of high.take(3)
                items(displayedHighTasks, key = { task: Task -> task.id }) { task ->
                    PriorityTaskRow(
                        task = task,
                        onToggle = { pos -> onTaskComplete(task.id, pos) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                        onPriorityChange = { newPriority ->
                            viewModel.updatePriority(
                                task.id,
                                newPriority
                            )
                        },
                        settings = settings  // Pass settings to task row
                    )
                }
            }

            // --- ACTIVE Section (Medium Priority) ---
            if (medium.isNotEmpty()) {
                item(key = "header_active") {
                    PrioritySectionHeader(
                        title = "☑ Active",
                        isExpanded = isActiveExpanded,
                        onToggle = { isActiveExpanded = !isActiveExpanded }
                    )
                }

                if (isActiveExpanded) {
                    items(medium, key = { task: Task -> task.id }) { task ->
                        PriorityTaskRow(
                            task = task,
                            onToggle = { pos -> onTaskComplete(task.id, pos) },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                            onPriorityChange = { newPriority ->
                                viewModel.updatePriority(
                                    task.id,
                                    newPriority
                                )
                            },
                            settings = settings  // Pass settings to task row
                        )
                    }
                }
            }

            // --- LATER/OPTIONAL Section (Low Priority) ---
            // Only show if setting is OFF or there are low priority tasks to show
            if (low.isNotEmpty() && !settings.hideLowPriorityInPriorityScreen) {
                item(key = "header_later") {
                    PrioritySectionHeader(
                        title = "Later/Optional",
                        iconRes = R.drawable.hourglass,
                        iconSize = 18.dp,
                        isExpanded = isLaterExpanded,
                        onToggle = { isLaterExpanded = !isLaterExpanded }
                    )
                }

                if (isLaterExpanded) {
                    items(low, key = { task: Task -> task.id }) { task ->
                        PriorityTaskRow(
                            task = task,
                            onToggle = { pos -> onTaskComplete(task.id, pos) },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                            onPriorityChange = { newPriority ->
                                viewModel.updatePriority(
                                    task.id,
                                    newPriority
                                )
                            },
                            settings = settings  // Pass settings to task row
                        )
                    }
                }
            }
        }
        // adding confetti overlay
        ConfettiOverlay(
            explosions = explosions,
            onBurstFinished = { id -> explosions.removeAll { it.id == id } }
        )
    }
}

@Composable
private fun PrioritySectionHeader(
    title: String,
    iconRes: Int? = null,
    iconSize: Dp = 30.dp,
    additionalCount: Int = 0,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .padding(end = 8.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.weight(1f))

        if (additionalCount > 0) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "+$additionalCount additional tasks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun PriorityTaskRow(
    task: Task,
    onToggle: (Offset) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPriorityChange: (String) -> Unit,
    settings: edu.fullsail.anchor.AppSettings  // Receive settings
) {
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // capture position for confetti
    var checkboxPosition by remember { mutableStateOf(Offset.Zero) }

    // Apply compact mode - reduce padding when enabled
    val cardHorizontalPadding = if (settings.compactMode) 12.dp else 16.dp
    val rowPadding = if (settings.compactMode) 4.dp else 8.dp
    val verticalPadding = if (settings.compactMode) 6.dp else 12.dp

    Card(modifier = Modifier.padding(horizontal = cardHorizontalPadding)) {  // Dynamic horizontal padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = rowPadding, end = 4.dp),  // Dynamic start padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle(checkboxPosition) }, // sending position
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    // calculate the center of the checkbox
                    val position = coordinates.positionInWindow()
                    val size = coordinates.size
                    checkboxPosition = Offset(
                        x = position.x + size.width / 2f,
                        y = position.y + size.height / 2f
                    )
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        vertical = verticalPadding,
                        horizontal = 8.dp
                    )  // Dynamic vertical padding
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (task.dueDate.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Task")
            }
            // Delete button - show dialog or delete immediately based on setting
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) {
                    showDeleteDialog = true
                } else {
                    onDelete()
                }
            }) {
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

    // Delete confirmation dialog (only shown if confirmBeforeDeleting is ON)
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