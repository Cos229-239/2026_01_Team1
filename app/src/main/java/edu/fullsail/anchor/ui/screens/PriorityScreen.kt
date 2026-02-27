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

// Helper data class for splitting tasks into four priority groups.
// Defined at file level (not inside the composable) so the Kotlin compiler
// can generate proper component1()–component4() destructuring functions.
private data class PriorityGroups(
    val high: List<Task>,
    val medium: List<Task>,
    val low: List<Task>,
    val unassigned: List<Task>
)

// =============================================================================
// PRIORITY SCREEN
// Displays all incomplete tasks split into three collapsible priority sections:
//   - Focus          (High priority) — capped at 3 tasks when the setting is ON
//   - Active         (Medium priority)
//   - Later/Optional (Low priority)  — hidden when "Hide Low Priority" setting is ON
//
// CONFETTI:
//   Checking off a task triggers a confetti burst at the checkbox's screen position.
//   All confetti state is managed via the explosions list; ConfettiOverlay renders
//   each burst and removes it via onBurstFinished when the animation finishes.
//
// BADGE EVALUATION:
//   A LaunchedEffect watches allTasks. Every time the task list changes (e.g. after
//   a completion), badge rules are re-evaluated and a Toast appears for any badge
//   that was just unlocked.
//
// COUNTDOWN TIMER:
//   Each task row now shows a live countdown to its due date via TaskCountdown,
//   added by a teammate. The timer updates every second via a coroutine loop.
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

    // explosions holds active confetti bursts. Each Explosion has a unique id and
    // a screen position. ConfettiOverlay animates them; finished ones are removed
    // by onBurstFinished so the list stays small.
    val explosions = remember { mutableStateListOf<Explosion>() }

    // The Box overlay's position in the device window. Subtracted from raw checkbox
    // positions so confetti bursts appear at the right spot relative to the overlay.
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }

    // Split incomplete tasks into four priority tiers in one remember block.
    // All four lists recompute together when allTasks changes.
    // "unassigned" catches tasks whose priority is "None" or any unrecognised value.
    val (high, medium, low, unassigned) = remember(allTasks) {
        val incomplete = allTasks.filter { !it.isCompleted }
        PriorityGroups(
            high       = incomplete.filter { it.priority == "High"   },
            medium     = incomplete.filter { it.priority == "Medium" },
            low        = incomplete.filter { it.priority == "Low"    },
            unassigned = incomplete.filter { it.priority == "None" || it.priority !in setOf("High","Medium","Low") }
        )
    }

    // When limitFocusToThree is ON, cap the Focus section at 3 tasks.
    // The full high list is still used to calculate the "+N additional" count in the header.
    val displayedHighTasks = remember(high, settings.limitFocusToThree) {
        if (settings.limitFocusToThree) high.take(3) else high
    }

    // Each section tracks its own expand/collapse state independently.
    // Using plain remember (not rememberSaveable) because Offset isn't serializable
    // and resetting expand state on process death is acceptable.
    var isFocusExpanded      by remember { mutableStateOf(true) }
    var isActiveExpanded     by remember { mutableStateOf(true) }
    var isLaterExpanded      by remember { mutableStateOf(true) }
    // Unassigned section starts collapsed — it's a catch-all, not a primary work queue
    var isUnassignedExpanded by remember { mutableStateOf(false) }

    // Task completion handler: toggles the task in Room, then adds a confetti burst
    // at the adjusted checkbox position. rawPosition comes from onGloballyPositioned
    // inside PriorityTaskRow; subtracting overlayOffset makes it relative to the Box.
    val onTaskComplete: (String, Offset) -> Unit = { taskId, rawPosition ->
        viewModel.toggleTaskCompletion(taskId)
        explosions.add(Explosion(id = System.nanoTime(), position = rawPosition - overlayOffset))
    }

    // Re-evaluate badges every time the task list changes.
    // LaunchedEffect(allTasks) re-runs the block whenever the list reference changes,
    // which happens after every task completion, deletion, or update from Room.
    LaunchedEffect(allTasks) {
        val stats = viewModel.buildEngagementStats()
        val (updatedBadges, newlyUnlocked) = BadgeRuleEngine.evaluate(
            stats    = stats,
            existing = badgesViewModel.badges
        )
        badgesViewModel.saveBadges(updatedBadges)

        // Toast only for badges unlocked in this evaluation pass, not all existing badges.
        newlyUnlocked.forEach { badge ->
            Toast.makeText(context, "Badge unlocked: ${badge.title}", Toast.LENGTH_SHORT).show()
        }
    }

    // The outer Box lets ConfettiOverlay float on top of the LazyColumn at full screen size.
    // onGloballyPositioned captures the Box's screen-space origin for confetti math.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                overlayOffset = coordinates.positionInWindow()
            }
    ) {
        LazyColumn(
            contentPadding      = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // --- FOCUS section (High Priority) ---
            // The header badge shows how many tasks are hidden when the limit is active.
            item(key = "header_focus") {
                val additionalCount = if (settings.limitFocusToThree && high.size > 3) {
                    high.size - 3  // number of high-priority tasks not shown in the section
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
                // displayedHighTasks is already capped to 3 when limitFocusToThree is ON
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
            // Guard: only renders the header and items if there are medium-priority tasks.
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
            // Two guards: tasks must exist AND the "Hide Low Priority" setting must be OFF.
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

            // --- UNASSIGNED section (priority == "None") ---
            // Catches tasks created without selecting a priority tier.
            // Collapsed by default so it doesn't compete with the active work sections.
            if (unassigned.isNotEmpty()) {
                item(key = "header_unassigned") {
                    PrioritySectionHeader(
                        title      = "Unassigned",
                        isExpanded = isUnassignedExpanded,
                        onToggle   = { isUnassignedExpanded = !isUnassignedExpanded }
                    )
                }

                if (isUnassignedExpanded) {
                    items(unassigned, key = { task: Task -> task.id }) { task ->
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

        } // end LazyColumn content

        // ConfettiOverlay floats above all content inside the Box.
        // onBurstFinished removes the finished explosion from the list so it stops rendering.
        ConfettiOverlay(
            explosions      = explosions,
            onBurstFinished = { id -> explosions.removeAll { it.id == id } }
        )
    } // end Box
}

// =============================================================================
// PRIORITY SECTION HEADER
// A tappable full-width row that labels each priority section. Features:
//   - Optional icon (PNG drawable) on the left
//   - Section title
//   - "+N additional tasks" label when the Focus cap is hiding tasks
//   - Animated chevron that rotates 180° when the section expands/collapses
//
// Tapping anywhere on the row calls onToggle, which flips the section's expand state.
// =============================================================================
@Composable
private fun PrioritySectionHeader(
    title: String,
    iconRes: Int? = null,       // Optional drawable (e.g. R.drawable.focus)
    iconSize: Dp = 30.dp,
    additionalCount: Int = 0,   // Hidden task count shown in Focus header when limit is active
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    // animateFloatAsState smoothly rotates the chevron icon between collapsed (0°) and expanded (180°).
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label       = "chevron rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)  // entire row is tappable, not just the icon
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
        Text(text = title, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.weight(1f))

        // Only visible in the Focus header when limitFocusToThree is ON and tasks are hidden
        if (additionalCount > 0) {
            Text(
                text     = "+$additionalCount additional tasks",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // graphicsLayer applies rotation without triggering a layout pass — more efficient
        // than swapping between two different icon composables.
        Icon(
            imageVector        = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier           = Modifier.graphicsLayer { rotationZ = rotation }
        )
    }
}

// =============================================================================
// PRIORITY TASK ROW
// A single task card on the Priority screen. Shows:
//   - Checkbox (triggers completion + confetti at the checkbox's exact screen position)
//   - Task title and due date label
//   - Live countdown timer via TaskCountdown (added by teammate)
//   - Edit button → Create/Edit Task screen
//   - Delete button (immediate or confirmation dialog based on the setting)
//   - Three-dot menu to change priority without opening the full edit screen
//
// Compact mode scales all padding values down when settings.compactMode is true.
// =============================================================================
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
private fun PriorityTaskRow(
    task: Task,
    onToggle: (Offset) -> Unit,         // receives the checkbox's screen-space center position
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPriorityChange: (String) -> Unit,
    settings: edu.fullsail.anchor.AppSettings
) {
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }

    // Updated by onGloballyPositioned on the Checkbox so confetti fires from
    // the center of the checkbox rather than from a fixed offset.
    var checkboxPosition by remember { mutableStateOf(Offset.Zero) }

    // Scale padding based on the compact mode setting
    val cardHorizontalPadding = if (settings.compactMode) 12.dp else 16.dp
    val rowPadding            = if (settings.compactMode) 4.dp  else 8.dp
    val verticalPadding       = if (settings.compactMode) 6.dp  else 12.dp

    Card(modifier = Modifier.padding(horizontal = cardHorizontalPadding)) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(start = rowPadding, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox — measures its own center position every time it's laid out so
            // confetti always bursts from exactly where the user tapped.
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
                // Due date label — only shown when a due date has been set
                if (task.dueDate.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)

                    // Live countdown added by teammate — ticks every second until the due date passes
                    TaskCountdown(dueDateMillis = task.dueDateMillis)
                }
            }

            // Edit — opens CreateTaskScreen with this task's data pre-loaded
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Task")
            }

            // Delete — either shows a confirmation dialog or deletes immediately,
            // depending on the confirmBeforeDeleting setting.
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) showDeleteDialog = true else onDelete()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }

            // Three-dot priority menu — reassigns the task to a different priority tier
            // without requiring the user to open the full edit screen.
            Box {
                IconButton(onClick = { showPriorityMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Change Priority")
                }
                DropdownMenu(
                    expanded         = showPriorityMenu,
                    onDismissRequest = { showPriorityMenu = false }
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

    // Delete confirmation dialog — only shown when the setting is ON
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

    // Edit confirmation dialog — confirms navigation to the edit screen
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

// =============================================================================
// TASK COUNTDOWN  (added by teammate)
// Shows a live "Ends in: Xd Xh Xm Xs" countdown below the due date label.
// Once the due date passes, it switches to "Overdue!" in red.
//
// HOW IT WORKS:
//   - remainingTime starts as (dueDateMillis - now) and is recalculated every second.
//   - LaunchedEffect(dueDateMillis) creates a coroutine that loops with a 1-second delay.
//     The effect restarts automatically if dueDateMillis changes (e.g. task is edited).
//   - The coroutine loop exits when remainingTime hits 0 so it doesn't run indefinitely.
//   - Days, hours, minutes, and seconds are computed with modulo arithmetic from the ms value.
//   - Days and hours are only shown in the string when they are non-zero, so a task due
//     in 3 minutes shows "3m 12s" instead of "0d 0h 3m 12s".
// =============================================================================
@Composable
fun TaskCountdown(dueDateMillis: Long?) {
    // Nothing to show when no due date is set
    if (dueDateMillis == null) return

    // remainingTime is recalculated every second by the LaunchedEffect below
    var remainingTime by remember { mutableLongStateOf(dueDateMillis - System.currentTimeMillis()) }

    // key1 = dueDateMillis restarts this coroutine if the due date changes
    LaunchedEffect(key1 = dueDateMillis) {
        while (remainingTime > 0) {
            delay(1000L)  // wait one second between updates
            remainingTime = dueDateMillis - System.currentTimeMillis()
        }
    }

    if (remainingTime > 0) {
        // Break milliseconds into human-readable units
        val seconds = (remainingTime / 1000) % 60
        val minutes = (remainingTime / (1000 * 60)) % 60
        val hours   = (remainingTime / (1000 * 60 * 60)) % 24
        val days    = (remainingTime / (1000 * 60 * 60 * 24))

        // Build the display string, omitting days and hours when they are zero
        val timeString = buildString {
            if (days > 0)            append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m ${seconds}s")
        }

        Text(
            text       = "Ends in: $timeString",
            style      = MaterialTheme.typography.labelSmall,
            color      = MaterialTheme.colorScheme.error,  // red to create urgency
            fontWeight = FontWeight.Bold
        )
    } else {
        // Due date has passed — show a persistent overdue indicator
        Text(
            text  = "Overdue!",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}