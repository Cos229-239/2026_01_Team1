package edu.fullsail.anchor.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import edu.fullsail.anchor.Task
import edu.fullsail.anchor.TaskViewModel
import edu.fullsail.anchor.engagement.badges.BadgeRuleEngine
import edu.fullsail.anchor.engagement.badges.BadgesViewModel
import edu.fullsail.anchor.engagement.badges.ConfettiOverlay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import edu.fullsail.anchor.engagement.badges.Explosion
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import edu.fullsail.anchor.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

// =============================================================================
// PRIORITY SCREEN
// Displays all incomplete tasks sorted into three priority sections:
//   - Focus     (High priority) — respects the "Limit Focus to 3" setting
//   - Active    (Medium priority)
//   - Later/Optional (Low priority) — hidden when the "Hide Low Priority" setting is ON
//
// Each section is independently collapsible. Completing a task from this screen
// triggers the confetti animation (handled by the badge system) and re-evaluates
// badge progress via a LaunchedEffect that watches the task list.
// =============================================================================
@Composable
fun PriorityScreen(
    navController: NavController,
    viewModel: TaskViewModel,
    badgesViewModel: BadgesViewModel,
    settingsViewModel: edu.fullsail.anchor.SettingsViewModel
) {
    val allTasks by viewModel.tasks.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val context  = LocalContext.current

    // Confetti explosion list — each Explosion represents one burst animation.
    // Managed by the badge system; do not remove.
    val explosions = remember { mutableStateListOf<Explosion>() }

    // Screen-space offset used to convert window-relative checkbox positions to
    // overlay-relative positions for accurate confetti placement.
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }

    // Pre-compute filtered and split task lists. Wrapped in remember so the lists
    // are only recalculated when allTasks changes, not on every recomposition.
    val (high, medium, low) = remember(allTasks) {
        val incomplete = allTasks.filter { !it.isCompleted }
        Triple(
            incomplete.filter { it.priority == "High" },
            incomplete.filter { it.priority == "Medium" },
            incomplete.filter { it.priority == "Low" }
        )
    }

    // Apply the "Limit Focus to 3" setting — only the first 3 high-priority tasks
    // are shown in the Focus section when the setting is ON.
    val displayedHighTasks = remember(high, settings.limitFocusToThree) {
        if (settings.limitFocusToThree) high.take(3) else high
    }

    // Section collapse state — each section starts expanded.
    // Using remember (not rememberSaveable) to avoid serialization issues with Offset types.
    var isFocusExpanded  by remember { mutableStateOf(true) }
    var isActiveExpanded by remember { mutableStateOf(true) }
    var isLaterExpanded  by remember { mutableStateOf(true) }

    // Completion handler — toggles task completion and triggers confetti at the
    // checkbox position. The badge system handles the confetti animation lifecycle.
    val onTaskComplete: (String, Offset) -> Unit = { taskId, rawPosition ->
        viewModel.toggleTaskCompletion(taskId)
        explosions.add(Explosion(id = System.nanoTime(), position = rawPosition - overlayOffset))
    }

    // Re-evaluate badge progress whenever the task list changes (e.g. after a completion).
    // LaunchedEffect is used here because badge evaluation reads from the ViewModel
    // and must run after the task state has settled.
    LaunchedEffect(allTasks) {
        val stats = viewModel.buildEngagementStats()
        val (updatedBadges, newlyUnlocked) = BadgeRuleEngine.evaluate(
            stats    = stats,
            existing = badgesViewModel.badges
        )
        badgesViewModel.saveBadges(updatedBadges)

        // Show a toast for each badge that was just unlocked
        newlyUnlocked.forEach { badge ->
            Toast.makeText(context, "Badge unlocked: ${badge.title}", Toast.LENGTH_SHORT).show()
        }
    }

    // Wrap everything in a Box so the confetti overlay can be positioned on top
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                // Track the screen position of this container for confetti offset math
                overlayOffset = coordinates.positionInWindow()
            }
    ) {
        LazyColumn(
            contentPadding      = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // --- FOCUS section (High Priority) ---
            // The header shows "+N additional tasks" when the limit is ON and
            // there are more high-priority tasks than the limit of 3.
            item(key = "header_focus") {
                val additionalCount = if (settings.limitFocusToThree && high.size > 3) {
                    high.size - 3
                } else {
                    0
                }
                PrioritySectionHeader(
                    title           = "Focus",
                    iconRes         = R.drawable.focus,
                    iconSize        = 30.dp,
                    additionalCount = additionalCount,
                    isExpanded      = isFocusExpanded,
                    onToggle        = { isFocusExpanded = !isFocusExpanded }
                )
            }

            if (isFocusExpanded) {
                items(displayedHighTasks, key = { task: Task -> task.id }) { task ->
                    PriorityTaskRow(
                        task             = task,
                        onToggle         = { pos -> onTaskComplete(task.id, pos) },
                        onDelete         = { viewModel.deleteTask(task.id) },
                        onEdit           = { navController.navigate("create_task_screen?taskId=${task.id}") },
                        onPriorityChange = { newPriority -> viewModel.updatePriority(task.id, newPriority) },
                        settings         = settings
                    )
                }
            }

            // --- ACTIVE section (Medium Priority) ---
            // Only rendered if there are medium-priority tasks to display.
            if (medium.isNotEmpty()) {
                item(key = "header_active") {
                    PrioritySectionHeader(
                        title      = "☑ Active",
                        isExpanded = isActiveExpanded,
                        onToggle   = { isActiveExpanded = !isActiveExpanded }
                    )
                }

                if (isActiveExpanded) {
                    items(medium, key = { task: Task -> task.id }) { task ->
                        PriorityTaskRow(
                            task             = task,
                            onToggle         = { pos -> onTaskComplete(task.id, pos) },
                            onDelete         = { viewModel.deleteTask(task.id) },
                            onEdit           = { navController.navigate("create_task_screen?taskId=${task.id}") },
                            onPriorityChange = { newPriority -> viewModel.updatePriority(task.id, newPriority) },
                            settings         = settings
                        )
                    }
                }
            }

            // --- LATER/OPTIONAL section (Low Priority) ---
            // Hidden entirely when the "Hide Low Priority" setting is ON.
            if (low.isNotEmpty() && !settings.hideLowPriorityInPriorityScreen) {
                item(key = "header_later") {
                    PrioritySectionHeader(
                        title      = "Later/Optional",
                        iconRes    = R.drawable.hourglass,
                        iconSize   = 18.dp,
                        isExpanded = isLaterExpanded,
                        onToggle   = { isLaterExpanded = !isLaterExpanded }
                    )
                }

                if (isLaterExpanded) {
                    items(low, key = { task: Task -> task.id }) { task ->
                        PriorityTaskRow(
                            task             = task,
                            onToggle         = { pos -> onTaskComplete(task.id, pos) },
                            onDelete         = { viewModel.deleteTask(task.id) },
                            onEdit           = { navController.navigate("create_task_screen?taskId=${task.id}") },
                            onPriorityChange = { newPriority -> viewModel.updatePriority(task.id, newPriority) },
                            settings         = settings
                        )
                    }
                }
            }
        }

        // Confetti overlay — renders burst animations above all other content.
        // onBurstFinished removes finished explosions from the list to free memory.
        ConfettiOverlay(
            explosions      = explosions,
            onBurstFinished = { id -> explosions.removeAll { it.id == id } }
        )
    }
}

// =============================================================================
// PRIORITY SECTION HEADER
// A tappable row that shows the section title, an optional icon, and a chevron
// that rotates to indicate whether the section is expanded or collapsed.
// When the section's task limit is exceeded (Focus + limitFocusToThree), an
// "+N additional tasks" label appears to let the user know tasks are hidden.
// =============================================================================
@Composable
private fun PrioritySectionHeader(
    title: String,
    iconRes: Int? = null,           // Optional drawable resource (e.g. focus.png, hourglass.png)
    iconSize: Dp = 30.dp,
    additionalCount: Int = 0,       // Number of tasks hidden by the Focus limit
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    // Animate the chevron rotation between 0° (collapsed) and 180° (expanded)
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label       = "chevron rotation"
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
                painter            = painterResource(id = iconRes),
                contentDescription = null,
                modifier           = Modifier.size(iconSize).padding(end = 8.dp)
            )
        }
        Text(
            text  = title,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.weight(1f))

        // Show the hidden-task count only when the Focus limit is active
        if (additionalCount > 0) {
            Text(
                text     = "+$additionalCount additional tasks",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Chevron icon rotates with animation to signal expand/collapse state
        Icon(
            imageVector        = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier           = Modifier.graphicsLayer { rotationZ = rotation }
        )
    }
}

// =============================================================================
// PRIORITY TASK ROW
// A compact task card used inside the Priority screen. Displays:
//   - Checkbox to complete the task (triggers confetti animation at its position)
//   - Task title and optional due date
//   - Edit button (navigates to CreateTaskScreen with the task's ID)
//   - Delete button (respects the confirmBeforeDeleting setting)
//   - Priority change menu (three-dot menu to reassign High/Medium/Low)
//
// Padding adjusts automatically based on the compactMode setting.
// =============================================================================
@Composable
private fun PriorityTaskRow(
    task: Task,
    onToggle: (Offset) -> Unit,         // Called with the checkbox's screen position for confetti
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPriorityChange: (String) -> Unit,
    settings: edu.fullsail.anchor.AppSettings
) {
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }

    // Track the checkbox's screen position so confetti bursts from the right spot
    var checkboxPosition by remember { mutableStateOf(Offset.Zero) }

    // Compact mode reduces padding so more tasks fit on screen without scrolling
    val cardHorizontalPadding = if (settings.compactMode) 12.dp else 16.dp
    val rowPadding            = if (settings.compactMode) 4.dp  else 8.dp
    val verticalPadding       = if (settings.compactMode) 6.dp  else 12.dp

    Card(modifier = Modifier.padding(horizontal = cardHorizontalPadding)) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(start = rowPadding, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox — triggers task completion and fires confetti at its center position
            Checkbox(
                checked        = task.isCompleted,
                onCheckedChange = { onToggle(checkboxPosition) },
                modifier       = Modifier.onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInWindow()
                    val size     = coordinates.size
                    checkboxPosition = Offset(
                        x = position.x + size.width  / 2f,
                        y = position.y + size.height / 2f
                    )
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = verticalPadding, horizontal = 8.dp)
            ) {
                Text(
                    text       = task.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                // Due date label — hidden when no due date is set
                if (task.dueDate.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)

                    // Adding the countdown timer here
                    TaskCountdown(dueDateMillis = task.dueDateMillis)
                }
            }

            // Edit button — opens the Create/Edit Task screen with this task's data pre-loaded
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Task")
            }

            // Delete button — shows a confirmation dialog if the setting is ON
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) showDeleteDialog = true else onDelete()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }

            // Three-dot priority menu — lets the user reassign the task's priority tier
            Box {
                IconButton(onClick = { showPriorityMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Change Priority")
                }
                DropdownMenu(
                    expanded          = showPriorityMenu,
                    onDismissRequest  = { showPriorityMenu = false }
                ) {
                    DropdownMenuItem(
                        text    = { Text("High Priority") },
                        onClick = { onPriorityChange("High");   showPriorityMenu = false }
                    )
                    DropdownMenuItem(
                        text    = { Text("Medium Priority") },
                        onClick = { onPriorityChange("Medium"); showPriorityMenu = false }
                    )
                    DropdownMenuItem(
                        text    = { Text("Low Priority") },
                        onClick = { onPriorityChange("Low");    showPriorityMenu = false }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog — only shown when confirmBeforeDeleting is ON
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete Task") },
            text             = { Text("Are you sure you want to delete this task?") },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Delete") }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit confirmation dialog — gives the user a chance to confirm before navigating away
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title            = { Text("Edit Task") },
            text             = { Text("Are you sure you want to edit this task?") },
            confirmButton    = {
                TextButton(onClick = { onEdit(); showEditDialog = false }) { Text("Edit") }
            },
            dismissButton    = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Countdown Timer
@Composable
fun TaskCountdown(dueDateMillis: Long?) {
    if (dueDateMillis == null)
        return
    // going to make it recalculate every 1 second
    var remainingTime by remember { mutableStateOf(dueDateMillis - System.currentTimeMillis()) }

    LaunchedEffect(key1 = dueDateMillis) {
        while (remainingTime > 0) {
            delay(1000L)
            remainingTime = dueDateMillis - System.currentTimeMillis()
        }
    }
    // going to make it show days hours minutes and seconds.
    if (remainingTime > 0) {
        val seconds = (remainingTime / 1000) % 60
        val minutes = (remainingTime / (1000 * 60)) % 60
        val hours = (remainingTime / (1000 * 60 * 60)) % 24
        val days = (remainingTime / (1000 * 60 * 60 * 24))
    // this is the time string showing days hours minutes and seconds.
    // gonna add something for once the timer runs out.
        val timeString = buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m ${seconds}s")
        }

        Text(
            text = "Ends in: $timeString",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error, // putting in red to create urgency
            fontWeight = FontWeight.Bold
        )
    } else {
        // once the timer runs out show "overdue!"
        Text(
            text = "Overdue!",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}