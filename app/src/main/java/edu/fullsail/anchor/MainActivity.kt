package edu.fullsail.anchor

// REQUIRED FOR NOTIFICATIONS
import edu.fullsail.anchor.notifications.createAnchorNotificationChannel
// REQUIRED FOR ROOM BADGE STORAGE
import edu.fullsail.anchor.engagement.badges.RoomBadgeRepository
import edu.fullsail.anchor.engagement.badges.BadgesViewModelFactory
// REQUIRED FOR THEME DATASTORE
import edu.fullsail.anchor.data.SettingsDataStore
import edu.fullsail.anchor.data.TaskRepository
import edu.fullsail.anchor.data.AnchorDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.fullsail.anchor.ui.screens.PriorityScreen
import edu.fullsail.anchor.ui.screens.SettingsScreen
import edu.fullsail.anchor.ui.theme.AnchorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import edu.fullsail.anchor.engagement.badges.BadgesViewModel
import edu.fullsail.anchor.engagement.badges.BadgeRuleEngine
// ADDED FOR DRAG & DROP
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initializes notification channel
        createAnchorNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            // Hoist settingsViewModel here so theme reacts to changes immediately.
            // The same instance is passed into AppNavigation to avoid a duplicate ViewModel.
            // REQUIRED FOR THEME DATASTORE — factory injects SettingsDataStore so theme persists
            val context = androidx.compose.ui.platform.LocalContext.current
            val settingsViewModelFactory = remember {
                SettingsViewModelFactory(SettingsDataStore(context))
            }
            val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
            val settings by settingsViewModel.settings.collectAsState()

            // Resolve dark theme from user setting.
            // branch here is to also swap color schemes.
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (settings.themeMode) {
                "Light" -> false
                "Dark"  -> true
                else    -> systemDark  // "System"
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !isDarkTheme
            }
            AnchorTheme(
                useDarkTheme = isDarkTheme,
                colorProfile = settings.colorProfile
            ) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(3000) // 3 seconds
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                } else {
                    AppNavigation(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}

// Replace the existing AppNavigation composable with this version.
// All content inside (Scaffold, NavHost, composable routes, etc.) is
// IDENTICAL to the original — only the taskViewModel creation line changes.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()

    // ADDED FOR PERSISTENCE — obtain application context to build the database
    val context = androidx.compose.ui.platform.LocalContext.current

    // ADDED FOR PERSISTENCE — build the repository from the singleton Room database
    val taskRepository = remember {
        TaskRepository(AnchorDatabase.getInstance(context).taskDao())
    }

    // ADDED FOR PERSISTENCE — factory needed because TaskViewModel now has a constructor param
    val taskViewModelFactory = remember { TaskViewModelFactory(taskRepository) }

    // MODIFIED FOR PERSISTENCE — pass the factory so the ViewModel receives the repository
    // Previously: val taskViewModel: TaskViewModel = viewModel()
    val taskViewModel: TaskViewModel = viewModel(factory = taskViewModelFactory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    /*
    Shared BadgesViewModel instance so task completion can trigger badge evaluation.
    Note (Gamification): Keep this BadgesViewModel created at the navigation level.
    Task completion uses this shared instance to eval/update badges.
    Removing / Moving this line will break tasks -> badge progress / unlocks.
    */
    // MODIFIED FOR PERSISTENCE — build RoomBadgeRepository and pass via factory
    // Previously: val badgesViewModel: BadgesViewModel = viewModel()
    // REQUIRED FOR ROOM BADGE STORAGE
    val badgeRepo = remember {
        RoomBadgeRepository(AnchorDatabase.getInstance(context).badgeProgressDao())
    }
    // REQUIRED FOR ROOM BADGE STORAGE
    val badgesViewModelFactory = remember { BadgesViewModelFactory(badgeRepo) }
    // REQUIRED FOR ROOM BADGE STORAGE
    val badgesViewModel: BadgesViewModel = viewModel(factory = badgesViewModelFactory)

    Scaffold(
        topBar = {
            if (currentRoute in listOf("tasks_screen", "priority_screen", "badges_screen", "settings_screen")) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentRoute) {
                                "tasks_screen"    -> "Tasks"
                                "priority_screen" -> "Priority"
                                "badges_screen"   -> "Badges"
                                "settings_screen" -> "Settings"
                                else              -> ""
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            if (currentRoute in listOf("tasks_screen", "priority_screen")) {
                FloatingActionButton(
                    onClick        = { navController.navigate("create_task_screen") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "priority_screen",
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable("tasks_screen") {
                TasksScreen(navController, taskViewModel, badgesViewModel, settingsViewModel)
            }
            composable("priority_screen") {
                PriorityScreen(navController, taskViewModel, badgesViewModel, settingsViewModel)
            }
            composable("badges_screen") {
                edu.fullsail.anchor.engagement.badges.BadgesScreen(badgesViewModel)
            }
            composable("settings_screen") { SettingsScreen(settingsViewModel) }
            composable(
                route = "create_task_screen?taskId={taskId}",
                arguments = listOf(navArgument("taskId") {
                    type     = NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                CreateTaskScreen(
                    navController     = navController,
                    taskViewModel     = taskViewModel,
                    taskId            = taskId,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

//--- TASKS SCREEN ---
// Added settingsViewModel parameter
// =============================================================================
// TASKS SCREEN
// Added features:
//   1. Active tasks sorted Daily > Weekly > Monthly > Yearly, then High > Medium > Low
//   2. Drag & drop reordering — long-press the drag handle to reorder active tasks
//   3. Completed Tasks section — collapsible, default collapsed, shows completion date
//   4. Subtasks — inline expandable checklist per task (see TaskItem)
// =============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    badgesViewModel: BadgesViewModel,
    settingsViewModel: SettingsViewModel
) {
    val allTasks by taskViewModel.tasks.collectAsState()
    val settings  by settingsViewModel.settings.collectAsState()

    val timeframeOrder = listOf("Daily", "Weekly", "Monthly", "Yearly")

    // Split tasks: completed always reads directly from DB so it stays in sync.
    // Active tasks use a drag snapshot for instant visual feedback during gestures.
    val activeTasks    = allTasks.filter { !it.isCompleted }.sortedBy { it.sortOrder }
    val completedTasks = allTasks.filter {  it.isCompleted }.sortedByDescending { it.completedAtMillis }

    // DRAG SNAPSHOT — null = read from DB, non-null = mid-drag override
    var dragSnapshot by remember { mutableStateOf<List<Task>?>(null) }
    val displayActiveTasks = dragSnapshot ?: activeTasks

    // Snackbar for move confirmations
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // After drag ends: persist sortOrder changes + any timeframe changes, then show confirmation
    LaunchedEffect(dragSnapshot) {
        val snapshot = dragSnapshot ?: return@LaunchedEffect
        delay(200)
        val activeMap  = activeTasks.associateBy { it.id }
        val activeIds  = activeMap.keys
        val snapshotIds = snapshot.map { it.id }.toSet()
        if (activeIds == snapshotIds) {
            // Detect tasks whose timeframe changed during the drag
            val timeframeChanges = snapshot.filter { s -> activeMap[s.id]?.timeframe != s.timeframe }
            // Persist timeframe changes first (updateTask preserves all other fields)
            timeframeChanges.forEach { task ->
                taskViewModel.updateTask(task.id, task.title, task.dueDateMillis, task.priority, task.timeframe)
            }
            // Persist new sort order
            taskViewModel.reorderTasks(snapshot)
            // Confirmation snackbar when a task moved to a different section
            if (timeframeChanges.size == 1) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message  = "\"${timeframeChanges[0].title}\" moved to ${timeframeChanges[0].timeframe}",
                        duration = SnackbarDuration.Short
                    )
                }
            } else if (timeframeChanges.size > 1) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message  = "${timeframeChanges.size} tasks moved",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
        dragSnapshot = null
    }

    // Timeframe sections independently collapsible, all expanded by default
    val timeframeExpanded = remember { mutableStateMapOf<String, Boolean>() }
    // Completed section collapsed by default
    var completedExpanded by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // DRAG & DROP
    // Cross-section support: when a task is dragged onto a header or into a different
    // timeframe group, its timeframe field is updated in the snapshot so the section
    // change persists when the drag ends. Priority sort is intentionally removed so
    // drag order within a section is respected and preserved.
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey   = to.key   as? String ?: return@rememberReorderableLazyListState

        // Never drag a non-task item, and never drag into the completed section
        if (fromKey.startsWith("hdr_") ||
            fromKey.startsWith("done_") || fromKey == "completed_header" ||
            toKey.startsWith("done_")   || toKey   == "completed_header") {
            return@rememberReorderableLazyListState
        }

        val current  = dragSnapshot ?: activeTasks
        val fromIdx  = current.indexOfFirst { it.id == fromKey }
        if (fromIdx < 0) return@rememberReorderableLazyListState

        val fromTask = current[fromIdx]

        // Determine the timeframe of the drop target.
        // If dropping onto a section header ("hdr_Weekly"), use that timeframe.
        // If dropping onto another task, use that task's current timeframe.
        val targetTimeframe = when {
            toKey.startsWith("hdr_") -> toKey.removePrefix("hdr_")
            else -> current.find { it.id == toKey }?.timeframe ?: fromTask.timeframe
        }

        // Update the dragged task's timeframe if it crossed a section boundary
        val movedTask = if (targetTimeframe != fromTask.timeframe) {
            fromTask.copy(timeframe = targetTimeframe)
        } else {
            fromTask
        }

        // Find the insertion index. When dropping on a header there's no matching task id,
        // so insert at the end of the target timeframe's current tasks.
        val toIdx = current.indexOfFirst { it.id == toKey }
        val newList = current.toMutableList()
        newList.removeAt(fromIdx)

        val insertAt = when {
            toIdx >= 0 -> {
                // Adjust for the removal above
                if (fromIdx < toIdx) toIdx - 1 else toIdx
            }
            else -> {
                // Dropped on a header — insert at the start of that section's tasks
                val firstInSection = newList.indexOfFirst { it.timeframe == targetTimeframe }
                if (firstInSection >= 0) firstInSection else newList.size
            }
        }

        newList.add(insertAt.coerceIn(0, newList.size), movedTask)
        dragSnapshot = newList
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (allTasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tasks yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap the + button below to add your first task.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                state               = lazyListState,
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                // --- Active tasks grouped by timeframe ---
                // NOTE: Priority sort intentionally removed so drag order within a
                // section is preserved. Change priority via the Edit Task screen.
                val grouped       = displayActiveTasks.groupBy { it.timeframe }
                val allTimeframes = (timeframeOrder + grouped.keys).distinct().filter { grouped.containsKey(it) }

                allTimeframes.forEach { timeframe ->
                    val tasksInGroup = grouped[timeframe] ?: return@forEach
                    val isExpanded   = timeframeExpanded.getOrDefault(timeframe, true)

                    // Collapsible section header with task count
                    item(key = "hdr_$timeframe", contentType = "header") {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = "$timeframe  (${tasksInGroup.size})",
                                style    = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { timeframeExpanded[timeframe] = !isExpanded }) {
                                Icon(
                                    imageVector        = if (isExpanded) Icons.Filled.KeyboardArrowUp
                                    else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse $timeframe" else "Expand $timeframe"
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // Task rows — keys must match ReorderableItem keys exactly (task.id)
                    if (isExpanded) {
                        items(tasksInGroup, key = { it.id }, contentType = { "task" }) { task ->
                            ReorderableItem(reorderState, key = task.id) { isDragging ->
                                val elevation = if (isDragging) 8.dp else 1.dp
                                Card(
                                    modifier  = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                                ) {
                                    TaskItem(
                                        task               = task,
                                        settings           = settings,
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        onEdit             = { navController.navigate("create_task_screen?taskId=${task.id}") },
                                        onDelete           = { taskViewModel.deleteTask(task.id) },
                                        onToggleComplete   = {
                                            taskViewModel.toggleTaskCompletion(task.id)
                                            /*
                                            BADGE SYSTEM BRIDGE:
                                            After a task is completed, we rebuild UserEngagementStats and re-evaluate badges.
                                            This keeps badge progress/unlocks in sync with real task behaviour.
                                            Do not remove without updating the badge evaluation pipeline.
                                            */
                                            val stats = taskViewModel.buildEngagementStats()
                                            val (updatedBadges, _) = BadgeRuleEngine.evaluate(
                                                stats    = stats,
                                                existing = badgesViewModel.badges
                                            )
                                            badgesViewModel.saveBadges(updatedBadges)
                                        },
                                        onAddSubtask    = { title -> taskViewModel.addSubtask(task.id, title) },
                                        onToggleSubtask = { subId -> taskViewModel.toggleSubtask(task.id, subId) },
                                        onDeleteSubtask = { subId -> taskViewModel.deleteSubtask(task.id, subId) }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Completed Tasks section ---
                if (completedTasks.isNotEmpty()) {
                    item(key = "completed_header", contentType = "header") {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = "Completed (${completedTasks.size})",
                                style    = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { completedExpanded = !completedExpanded }) {
                                Icon(
                                    imageVector        = if (completedExpanded) Icons.Filled.KeyboardArrowUp
                                    else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (completedExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    if (completedExpanded) {
                        items(completedTasks, key = { "done_${it.id}" }, contentType = { "completed" }) { task ->
                            CompletedTaskItem(
                                task      = task,
                                settings  = settings,
                                onUncheck = {
                                    dragSnapshot = null
                                    taskViewModel.toggleTaskCompletion(task.id)
                                    val stats = taskViewModel.buildEngagementStats()
                                    val (updatedBadges, _) = BadgeRuleEngine.evaluate(
                                        stats    = stats,
                                        existing = badgesViewModel.badges
                                    )
                                    badgesViewModel.saveBadges(updatedBadges)
                                },
                                onDelete  = { taskViewModel.deleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }

        // Snackbar shown after cross-section drags (e.g. "Task moved to Weekly")
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)
        )
    }
}

// --- Splash Screen ---
// =============================================================================
// COMPLETED TASK ITEM
// Simpler card: no edit, no drag handle.
// Shows strikethrough title + "Completed Jan 15" label (Option B).
// Checkbox unchecks (undoes) completion; delete respects confirmBeforeDeleting setting.
// =============================================================================
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun CompletedTaskItem(
    task: Task,
    settings: AppSettings,
    onUncheck: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val cardPadding = if (settings.compactMode) 6.dp else 10.dp

    // show the date the task was completed, e.g. "Completed Jan 15"
    val completedLabel = task.completedAtMillis?.let {
        "Completed ${Task.completedDateFormat.format(Date(it))}"
    } ?: "Completed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier          = Modifier.padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tap to undo completion
            Checkbox(checked = true, onCheckedChange = { onUncheck() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text           = task.title,
                    fontWeight     = FontWeight.Normal,
                    color          = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textDecoration = TextDecoration.LineThrough,
                    style          = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text  = completedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) showDeleteDialog = true else onDelete()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete Task") },
            text   = { Text("Delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

//--- TASK ITEM ---
// Added settings parameter for compact mode and confirm delete
// =============================================================================
// TASK ITEM
// Changes from original:
//   - Drag handle icon on the left (draggableHandle modifier injected from parent)
//   - Subtask progress badge "2/5" next to title
//   - Expand/collapse chevron for subtask section
//   - Expanded section: subtask checklist + delete button + add subtask text field
//   - "All subtasks done — mark task complete?" suggestion strip
// =============================================================================
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "ModifierParameter")
@Composable
fun TaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    settings: AppSettings, // Recieve Settings
    dragHandleModifier: Modifier = Modifier,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var subtasksExpanded by remember { mutableStateOf(false) }
    var newSubtaskTitle  by remember { mutableStateOf("") }

    val cardPadding    = if (settings.compactMode) 6.dp else 12.dp
    val doneCount      = task.subtasks.count { it.isDone }
    val totalCount     = task.subtasks.size
    val allSubtasksDone = totalCount > 0 && doneCount == totalCount

    Column(modifier = Modifier.fillMaxWidth()) {

        // ---- Main task row ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ADDED FOR DRAG & DROP — drag handle; long-press to reorder
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = dragHandleModifier // APPLY THE MODIFIER HERE
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleComplete() })
            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = task.title,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    // ADDED FOR SUBTASKS — progress badge, e.g. "2/5"
                    if (totalCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            color = if (allSubtasksDone) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text     = "$doneCount/$totalCount",
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color    = if (allSubtasksDone) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (task.dueDate.isNotBlank()) {
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)
                }
            }

            // ADDED FOR SUBTASKS — expand/collapse chevron
            IconButton(onClick = { subtasksExpanded = !subtasksExpanded }) {
                Icon(
                    imageVector = if (subtasksExpanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (subtasksExpanded) "Collapse subtasks" else "Expand subtasks"
                )
            }

            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Task")
            }
            // Delete button — show dialog or delete immediately based on setting
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) showDeleteDialog = true else onDelete()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
            }
        }

        // ---- Subtask section (animated expand / collapse) ----
        AnimatedVisibility(
            visible = subtasksExpanded,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, end = 12.dp, bottom = 8.dp)
            ) {
                // "All done" suggestion strip
                if (allSubtasksDone && !task.isCompleted) {
                    Surface(
                        color    = MaterialTheme.colorScheme.surface,
                        shape    = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = "All subtasks done! Mark task complete?",
                                style    = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color    = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = onToggleComplete) {
                                Text("Complete", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Existing subtask rows
                task.subtasks.forEach { subtask ->
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked         = subtask.isDone,
                            onCheckedChange = { onToggleSubtask(subtask.id) },
                            modifier        = Modifier.size(32.dp)
                        )
                        Text(
                            text           = subtask.title,
                            style          = MaterialTheme.typography.bodyMedium,
                            modifier       = Modifier.weight(1f),
                            color          = if (subtask.isDone)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null
                        )
                        IconButton(
                            onClick  = { onDeleteSubtask(subtask.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove subtask",
                                modifier           = Modifier.size(16.dp),
                                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // "Add subtask" inline text field
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value           = newSubtaskTitle,
                        onValueChange   = { newSubtaskTitle = it },
                        placeholder     = { Text("Add subtask...", style = MaterialTheme.typography.bodySmall) },
                        modifier        = Modifier.weight(1f),
                        singleLine      = true,
                        textStyle       = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newSubtaskTitle.isNotBlank()) {
                                onAddSubtask(newSubtaskTitle)
                                newSubtaskTitle = ""
                            }
                        })
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                onAddSubtask(newSubtaskTitle)
                                newSubtaskTitle = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add subtask")
                    }
                }
            }
        }
    }

    // ---- Dialogs ----
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete Task") },
            text   = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title  = { Text("Edit Task") },
            text   = { Text("Are you sure you want to edit this task?") },
            confirmButton = {
                TextButton(onClick = { onEdit(); showEditDialog = false }) { Text("Edit") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- Splash Screen ---
@Composable
fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = "Anchor",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "Anchor what matters today",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

//--- CREATE TASK SCREEN --- (unchanged from original)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    taskId: String?,
    settingsViewModel: SettingsViewModel
) {
    val isEditing  = taskId != null
    val taskToEdit = if (isEditing) taskViewModel.getTaskById(taskId!!) else null
    val settings  by settingsViewModel.settings.collectAsState()

    var titleInput       by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var dueDateMillis    by remember { mutableStateOf(taskToEdit?.dueDateMillis) }
    val priorityOptions   = listOf("High", "Medium", "Low")
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: settings.defaultPriority) }
    val timeframeOptions  = listOf("Daily", "Weekly", "Monthly", "Yearly")
    var selectedTimeframe by remember { mutableStateOf(taskToEdit?.timeframe ?: settings.defaultTimeframe) }
    var validationError  by remember { mutableStateOf<String?>(null) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = dueDateMillis?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY)
        } ?: 0,
        initialMinute = dueDateMillis?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE)
        } ?: 0
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Task" else "Create Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = "Task Title")
            OutlinedTextField(
                value         = titleInput,
                onValueChange = { titleInput = it },
                label         = { Text("Title") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Due Date")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showDatePicker = true },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = Color.White
                    )
                ) {
                    Text(text = dueDateMillis?.let {
                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Select a date")
                }
                Button(
                    onClick = { showTimePicker = true },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = Color.White
                    )
                ) {
                    Text(text = dueDateMillis?.let {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
                    } ?: "Select a time")
                }
            }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { selectedDate ->
                                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                    utcCal.timeInMillis = selectedDate
                                    val localCal = Calendar.getInstance()
                                    dueDateMillis?.let { localCal.timeInMillis = it }
                                    localCal.set(Calendar.YEAR,         utcCal.get(Calendar.YEAR))
                                    localCal.set(Calendar.MONTH,        utcCal.get(Calendar.MONTH))
                                    localCal.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                    dueDateMillis = localCal.timeInMillis
                                }
                                showDatePicker = false
                            },
                            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("OK", color = Color.White) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDatePicker = false },
                            colors  = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Cancel", color = Color.White) }
                    }
                ) { DatePicker(state = datePickerState) }
            }
            if (showTimePicker) {
                TimePickerDialog(
                    onDismissRequest = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        val cal = Calendar.getInstance()
                        dueDateMillis?.let { cal.timeInMillis = it }
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        dueDateMillis = cal.timeInMillis
                        showTimePicker = false
                    },
                    state = timePickerState
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Priority", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.fillMaxWidth()) {
                priorityOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selectedPriority == option, onClick = { selectedPriority = option })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedPriority == option, onClick = null)
                        Text(text = option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Timeframe", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.fillMaxWidth()) {
                timeframeOptions.chunked(2).forEach { rowOptions ->
                    Row(Modifier.fillMaxWidth()) {
                        rowOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(selected = selectedTimeframe == option, onClick = { selectedTimeframe = option }),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedTimeframe == option, onClick = null)
                                Text(text = option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // -------------- SAVE BUTTON ------------
            Button(
                onClick = {
                    val success = if (isEditing) {
                        taskViewModel.updateTask(taskId!!, titleInput, dueDateMillis, selectedPriority, selectedTimeframe)
                    } else {
                        taskViewModel.addTask(titleInput, dueDateMillis, selectedPriority, selectedTimeframe)
                    }
                    if (success) {
                        navController.popBackStack()
                    } else {
                        validationError = "Title cannot be empty."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = Color.White
                )
            ) {
                Text(if (isEditing) "Update Task" else "Save Task", fontSize = 16.sp)
            }
        }

        if (validationError != null) {
            AlertDialog(
                onDismissRequest = { validationError = null },
                title  = { Text("Invalid Input") },
                text   = { Text(validationError!!) },
                confirmButton = {
                    TextButton(onClick = { validationError = null }) { Text("OK") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    state: TimePickerState
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title  = { Text("Select Time") },
        text   = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.hour, state.minute) },
                colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) { Text("Cancel") }
        }
    )
}

//--- BOTTOM NAVIGATION BAR ---
@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon     = { Icon(Icons.Filled.List, contentDescription = "Tasks") },
            label    = { Text("Tasks") },
            selected = currentRoute == "tasks_screen",
            onClick  = {
                navController.navigate("tasks_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon     = { Icon(Icons.Filled.Flag, contentDescription = "Priority") },
            label    = { Text("Priority") },
            selected = currentRoute == "priority_screen",
            onClick  = {
                navController.navigate("priority_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon     = { Icon(Icons.Filled.Star, contentDescription = "Badges") },
            label    = { Text("Badges") },
            selected = currentRoute == "badges_screen",
            onClick  = {
                navController.navigate("badges_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        // Settings Screen
        NavigationBarItem(
            icon     = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label    = { Text("Settings") },
            selected = currentRoute == "settings_screen",
            onClick  = {
                navController.navigate("settings_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}