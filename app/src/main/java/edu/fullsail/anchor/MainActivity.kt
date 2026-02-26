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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

// =============================================================================
// MAIN ACTIVITY
// Entry point of the app. Responsibilities:
//   1. Register the notification channel so task reminders can be delivered.
//   2. Create the SettingsViewModel here (at the root) so the theme responds
//      to user changes immediately — passing it down avoids creating a second
//      instance inside AppNavigation.
//   3. Resolve the dark/light theme from the user's setting and the system default.
//   4. Update the status bar appearance to match the active theme.
//   5. Show a 3-second splash screen on first launch, then hand off to AppNavigation.
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register the Anchor notification channel. Must happen before any notification
        // is posted — safe to call repeatedly, Android ignores duplicate registrations.
        createAnchorNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            // Build SettingsViewModel at the root so theme changes apply immediately.
            // remember{} ensures the factory is only created once per composition lifecycle.
            val context = androidx.compose.ui.platform.LocalContext.current
            val settingsViewModelFactory = remember {
                SettingsViewModelFactory(SettingsDataStore(context))
            }
            val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
            val settings by settingsViewModel.settings.collectAsState()

            // Resolve dark theme: explicit "Light"/"Dark" overrides the system default.
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (settings.themeMode) {
                "Light" -> false
                "Dark"  -> true
                else    -> systemDark  // "System" — follow OS setting
            }

            // SideEffect runs after every successful recomposition. Used here to sync
            // the status bar icon color with the current theme without triggering recomposition.
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !isDarkTheme
            }

            AnchorTheme(
                useDarkTheme = isDarkTheme,
                colorProfile = settings.colorProfile
            ) {
                // Show the splash screen for 3 seconds, then transition to the main app.
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(3000)
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                } else {
                    // Pass the already-created settingsViewModel so AppNavigation doesn't
                    // create a second instance with a fresh (un-persisted) state.
                    AppNavigation(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}

// =============================================================================
// APP NAVIGATION
// Sets up the NavController, shared ViewModels, Scaffold structure (top bar,
// bottom nav, FAB), and the NavHost with all screen routes.
//
// WHY VIEWMODELS ARE CREATED HERE:
//   ViewModels must be scoped to the same lifecycle host to share state across
//   screens. Creating them here (not inside individual screens) means the same
//   TaskViewModel and BadgesViewModel instance is passed to every screen that
//   needs it, so task changes on the Tasks screen are immediately visible on
//   the Priority screen.
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Build the Room-backed repository once. remember{} prevents re-creating it
    // on every recomposition. AnchorDatabase.getInstance() uses a singleton pattern
    // so this always returns the same database object.
    val taskRepository = remember {
        TaskRepository(AnchorDatabase.getInstance(context).taskDao())
    }

    // Factory needed because TaskViewModel has a constructor parameter (the repository).
    // The default ViewModelProvider cannot inject constructor args without a factory.
    val taskViewModelFactory = remember { TaskViewModelFactory(taskRepository) }
    val taskViewModel: TaskViewModel = viewModel(factory = taskViewModelFactory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Build the badge repository and ViewModel using the same pattern as tasks.
    // IMPORTANT: This must stay here at the navigation level so all screens share
    // the same BadgesViewModel. Moving it into individual screens would break
    // badge evaluation since each screen would have its own isolated badge state.
    val badgeRepo = remember {
        RoomBadgeRepository(AnchorDatabase.getInstance(context).badgeProgressDao())
    }
    val badgesViewModelFactory = remember { BadgesViewModelFactory(badgeRepo) }
    val badgesViewModel: BadgesViewModel = viewModel(factory = badgesViewModelFactory)

    Scaffold(
        topBar = {
            // Show the top bar only on the four main screens, not on the create/edit screen.
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
            // FAB only appears on screens where creating a task makes sense
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
            // Priority screen is the home screen (agreed by team), shown after the splash.
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

            // Create/Edit screen — taskId is optional. When null, the screen creates a new task.
            // When non-null, the screen pre-fills fields with the existing task's data.
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

// =============================================================================
// TASKS SCREEN
// The main to-do list, split into:
//   - Active tasks grouped by timeframe (Daily → Weekly → Monthly → Yearly)
//   - Completed tasks section (collapsed by default, shows completion date)
//
// KEY FEATURES:
//   Drag & drop: long-press the drag handle to reorder tasks. Tasks can also be
//   dragged across section headers to change their timeframe. The drag snapshot
//   pattern gives instant visual feedback while the gesture is in progress,
//   then the final order is persisted to Room when the drag ends.
//
//   Subtasks: each task card has an expand/collapse chevron that reveals an
//   inline checklist. Subtasks can be added, checked, and removed without
//   navigating away from the task list.
//
//   Badge evaluation: runs after every task completion so badge progress stays
//   in sync without a separate background job.
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

    // The order in which sections are displayed. "None" is last so the Unassigned
    // section always appears below all assigned timeframe sections.
    val timeframeOrder = listOf("Daily", "Weekly", "Monthly", "Yearly", "None")

    // Maps priority strings to sort keys for the "By Priority" mode.
    // Int.MAX_VALUE is the fallback so any unrecognised value sorts to the bottom.
    val priorityOrder  = mapOf("High" to 0, "Medium" to 1, "Low" to 2, "None" to 3)

    // Separate completed from active. Completed tasks always read directly from the DB
    // so they stay accurate. Active tasks go through the drag snapshot (see below).
    val activeTasks    = allTasks.filter { !it.isCompleted }.sortedBy { it.sortOrder }
    val completedTasks = allTasks.filter {  it.isCompleted }.sortedByDescending { it.completedAtMillis }

    // DRAG SNAPSHOT PATTERN:
    //   null  = no drag in progress; the LazyColumn renders from Room via activeTasks.
    //   non-null = a drag is in progress; the LazyColumn renders this snapshot for
    //              instant visual feedback without waiting for a Room write.
    // When the drag ends, LaunchedEffect(dragSnapshot) persists the new order and
    // clears the snapshot back to null.
    var dragSnapshot by remember { mutableStateOf<List<Task>?>(null) }
    val displayActiveTasks = dragSnapshot ?: activeTasks

    // Snackbar shown when a task is dragged into a different timeframe section
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    // Runs when dragSnapshot becomes non-null and then goes back to null.
    // After a brief debounce (200ms), it:
    //   1. Detects which tasks changed timeframe during the drag
    //   2. Persists timeframe changes via updateTask()
    //   3. Persists the new sort order via reorderTasks()
    //   4. Shows a snackbar confirming any cross-section moves
    LaunchedEffect(dragSnapshot) {
        val snapshot = dragSnapshot ?: return@LaunchedEffect
        delay(200)  // debounce: wait until the drag gesture has fully settled
        val activeMap   = activeTasks.associateBy { it.id }
        val activeIds   = activeMap.keys
        val snapshotIds = snapshot.map { it.id }.toSet()
        if (activeIds == snapshotIds) {
            // Find tasks whose timeframe field changed during this drag
            val timeframeChanges = snapshot.filter { s -> activeMap[s.id]?.timeframe != s.timeframe }
            // Persist timeframe changes before reordering so the order write sees the right data
            timeframeChanges.forEach { task ->
                taskViewModel.updateTask(task.id, task.title, task.dueDateMillis, task.priority, task.timeframe)
            }
            taskViewModel.reorderTasks(snapshot)
            // Confirmation snackbar for cross-section drags
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
        dragSnapshot = null  // clear snapshot so the list reads from Room again
    }

    // Each timeframe section tracks its own expand/collapse state independently.
    // mutableStateMapOf lets us add entries lazily without pre-populating the map.
    val timeframeExpanded = remember { mutableStateMapOf<String, Boolean>() }
    // Completed section is collapsed by default to keep the list clean.
    var completedExpanded by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // DRAG & DROP REORDER LOGIC:
    // rememberReorderableLazyListState ties into the LazyColumn's scroll state.
    // The lambda runs on every drag frame: it reads from/to keys, validates that
    // neither is a non-task item (header or completed task), computes the new list
    // order and timeframe, then updates dragSnapshot so the UI redraws immediately.
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey   = to.key   as? String ?: return@rememberReorderableLazyListState

        // Prevent dragging headers or completed items
        if (fromKey.startsWith("hdr_") ||
            fromKey.startsWith("done_") || fromKey == "completed_header" ||
            toKey.startsWith("done_")   || toKey   == "completed_header") {
            return@rememberReorderableLazyListState
        }

        val current = dragSnapshot ?: activeTasks
        val fromIdx = current.indexOfFirst { it.id == fromKey }
        if (fromIdx < 0) return@rememberReorderableLazyListState

        val fromTask = current[fromIdx]

        // Determine the target timeframe:
        //   - If dropped on a section header ("hdr_Weekly"), use the header's timeframe.
        //   - If dropped on another task, use that task's current timeframe.
        val targetTimeframe = when {
            toKey.startsWith("hdr_") -> toKey.removePrefix("hdr_")
            else -> current.find { it.id == toKey }?.timeframe ?: fromTask.timeframe
        }

        // Copy the task with an updated timeframe if it crossed a section boundary
        val movedTask = if (targetTimeframe != fromTask.timeframe) {
            fromTask.copy(timeframe = targetTimeframe)
        } else {
            fromTask
        }

        // Compute the insertion index. When dropping on a header there is no matching
        // task id, so insert at the beginning of the target timeframe's task group.
        val toIdx = current.indexOfFirst { it.id == toKey }
        val newList = current.toMutableList()
        newList.removeAt(fromIdx)

        val insertAt = when {
            toIdx >= 0 -> if (fromIdx < toIdx) toIdx - 1 else toIdx  // adjust for the removal above
            else -> {
                // Dropped on a header — insert at the first position in that section
                val firstInSection = newList.indexOfFirst { it.timeframe == targetTimeframe }
                if (firstInSection >= 0) firstInSection else newList.size
            }
        }

        newList.add(insertAt.coerceIn(0, newList.size), movedTask)
        dragSnapshot = newList  // triggers immediate recomposition with the new order
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Show an empty state when there are no tasks at all (neither active nor completed)
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
                // "Soonest First" flattens everything into a single "All Tasks" group sorted
                // by due date. All other modes group by timeframe field.
                //
                // Within each group, the sort mode controls task order:
                //   Manual        — drag-and-drop sortOrder from Room
                //   By Priority   — High > Medium > Low > None (uses priorityOrder map)
                //   By Due Date   — ascending due date; nullsLast() pushes no-date tasks down
                //   Soonest First — already sorted at the group level above
                val isSoonestFirst = settings.sortMode == "Soonest First"

                // For Soonest First, one virtual group holds all tasks sorted by due date.
                // For all other modes, tasks are grouped normally by timeframe.
                val grouped: Map<String, List<Task>> = if (isSoonestFirst) {
                    mapOf("All Tasks" to displayActiveTasks.sortedWith(compareBy(nullsLast()) { it.dueDateMillis }))
                } else {
                    displayActiveTasks.groupBy { it.timeframe }
                }

                // Build the section list in canonical order (or just ["All Tasks"] for Soonest First).
                // (timeframeOrder + grouped.keys).distinct() keeps Daily→Weekly→Monthly→Yearly→None
                // order while appending any unexpected values at the end.
                val allTimeframes = if (isSoonestFirst) {
                    listOf("All Tasks")
                } else {
                    (timeframeOrder + grouped.keys).distinct().filter { grouped.containsKey(it) }
                }

                allTimeframes.forEach { timeframe ->
                    val rawGroup = grouped[timeframe] ?: return@forEach

                    // Apply per-section sorting based on the active sort mode.
                    // Soonest First is already sorted at the group level so it falls through to else.
                    val tasksInGroup = when (settings.sortMode) {
                        "By Priority" -> rawGroup.sortedBy { priorityOrder[it.priority] ?: Int.MAX_VALUE }
                        "By Due Date" -> rawGroup.sortedWith(compareBy(nullsLast()) { it.dueDateMillis })
                        else          -> rawGroup
                    }
                    val isExpanded = timeframeExpanded.getOrDefault(timeframe, true)

                    // Translate internal "None" to the user-facing label "Unassigned"
                    val headerLabel = when (timeframe) {
                        "None"      -> "Unassigned"
                        "All Tasks" -> "All Tasks"
                        else        -> timeframe
                    }

                    // Collapsible section header — "hdr_" prefix identifies headers to the
                    // drag-and-drop guard so they cannot be picked up and dragged.
                    item(key = "hdr_$timeframe", contentType = "header") {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text  = "$headerLabel  (${tasksInGroup.size})",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                // Subtitle hint on the Unassigned section so users know they can assign later
                                if (timeframe == "None") {
                                    Text(
                                        text  = "No timeframe set — edit a task to assign one",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                            }
                            // Toggle button flips expand state for this section only
                            IconButton(onClick = { timeframeExpanded[timeframe] = !isExpanded }) {
                                Icon(
                                    imageVector        = if (isExpanded) Icons.Filled.KeyboardArrowUp
                                    else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse $headerLabel" else "Expand $headerLabel"
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // Task rows — the key here (task.id) must exactly match the key used in
                    // ReorderableItem below, otherwise drag-and-drop state gets out of sync.
                    if (isExpanded) {
                        items(tasksInGroup, key = { it.id }, contentType = { "task" }) { task ->
                            ReorderableItem(reorderState, key = task.id) { isDragging ->
                                // Raise the card's shadow while it is being dragged
                                val elevation = if (isDragging) 8.dp else 1.dp
                                Card(
                                    modifier  = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                                ) {
                                    TaskItem(
                                        task               = task,
                                        settings           = settings,
                                        // draggableHandle() is the modifier that makes the drag handle icon
                                        // respond to long-press gestures from the reorder library.
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        onEdit             = { navController.navigate("create_task_screen?taskId=${task.id}") },
                                        onDelete           = { taskViewModel.deleteTask(task.id) },
                                        onToggleComplete   = {
                                            taskViewModel.toggleTaskCompletion(task.id)
                                            // Re-evaluate badges immediately after every completion
                                            // so the badge screen always shows up-to-date progress.
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
                // Only rendered when there is at least one completed task.
                // Collapsed by default so it doesn't compete with active tasks on first open.
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
                        // "done_" prefix distinguishes completed item keys from active task keys
                        // in the drag-and-drop guard so completed items cannot be reordered.
                        items(completedTasks, key = { "done_${it.id}" }, contentType = { "completed" }) { task ->
                            CompletedTaskItem(
                                task      = task,
                                settings  = settings,
                                onUncheck = {
                                    // Clear the snapshot before unchecking so the list doesn't
                                    // briefly show stale drag state when the task moves back to active.
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

        // Snackbar appears above the bottom nav bar after a cross-section drag
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)
        )
    }
}

// =============================================================================
// COMPLETED TASK ITEM
// A simplified card for the Completed section. No drag handle or subtask controls.
//   - Strikethrough title + dimmed text to visually distinguish from active tasks
//   - "Completed Jan 15" label using completedAtMillis for the date
//   - Checking the checkbox undoes the completion (moves task back to active)
//   - Delete respects the confirmBeforeDeleting setting
// =============================================================================
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_EXPRESSION")
@Composable
fun CompletedTaskItem(
    task: Task,
    settings: AppSettings,
    onUncheck: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val cardPadding = if (settings.compactMode) 6.dp else 10.dp

    // Format the completion timestamp as a short label, e.g. "Completed Jan 15"
    val completedLabel = task.completedAtMillis?.let {
        "Completed ${Task.completedDateFormat.format(Date(it))}"
    } ?: "Completed"

    // Dim the card background to visually separate completed from active tasks
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
            // Unchecking a completed task moves it back to the active list
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

// =============================================================================
// TASK ITEM
// The active task card on the Tasks screen. Includes:
//   - Drag handle (long-press to reorder; draggableHandle modifier injected from parent)
//   - Checkbox to complete the task (triggers badge evaluation)
//   - Task title, due date, and subtask progress badge ("2/5")
//   - Expand/collapse chevron for the subtask section
//   - Subtask section (animated): existing subtask rows + inline "Add subtask" field
//   - "All subtasks done — mark task complete?" suggestion strip
//   - Edit and Delete buttons (delete respects confirmBeforeDeleting setting)
// =============================================================================
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "ModifierParameter", "UNUSED_EXPRESSION")
@Composable
fun TaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    settings: AppSettings,
    dragHandleModifier: Modifier = Modifier,  // Injected by ReorderableItem; do not use Modifier.draggableHandle() directly here
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }
    var subtasksExpanded by remember { mutableStateOf(false) }  // chevron state
    var newSubtaskTitle  by remember { mutableStateOf("") }     // input field state

    val doneCount      = task.subtasks.count { it.isDone }
    val totalCount     = task.subtasks.size
    // True when every subtask is checked — used to show the "mark task complete?" strip
    val allSubtasksDone = totalCount > 0 && doneCount == totalCount

    Column(modifier = Modifier.fillMaxWidth()) {

        // ---- Main task row ----
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle — long-press initiates the reorder gesture.
            // dragHandleModifier is provided by ReorderableItem in TasksScreen;
            // applying it here is what connects this icon to the drag library.
            Icon(
                imageVector        = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier           = dragHandleModifier.size(24.dp),
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                    // Subtask progress badge — only visible when the task has subtasks.
                    // Badge turns the primary color when all subtasks are done.
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
                // Due date label — hidden when empty
                if (task.dueDate.isNotBlank()) {
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Chevron toggles the subtask section visibility
            IconButton(onClick = { subtasksExpanded = !subtasksExpanded }) {
                Icon(
                    imageVector        = if (subtasksExpanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (subtasksExpanded) "Collapse subtasks" else "Expand subtasks"
                )
            }

            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Task")
            }
            // Delete — dialog or immediate depending on the setting
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) showDeleteDialog = true else onDelete()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
            }
        }

        // ---- Subtask section (animated expand / collapse) ----
        // AnimatedVisibility slides the section in and out smoothly.
        // The content is only composed when visible = true, saving layout work.
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
                // "All done" suggestion strip — appears when every subtask is checked
                // but the parent task itself is still incomplete.
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
                            // Tapping "Complete" triggers the same completion flow as the checkbox
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
                        // Smaller checkbox (32dp) to keep subtask rows compact
                        Checkbox(
                            checked         = subtask.isDone,
                            onCheckedChange = { onToggleSubtask(subtask.id) },
                            modifier        = Modifier.size(32.dp)
                        )
                        Text(
                            text           = subtask.title,
                            style          = MaterialTheme.typography.bodyMedium,
                            modifier       = Modifier.weight(1f),
                            // Dim and strikethrough completed subtasks
                            color          = if (subtask.isDone)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null
                        )
                        // X button removes the subtask immediately without a confirmation dialog
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

                // Inline "Add subtask" text field.
                // Pressing Done on the keyboard or the + button appends a new subtask.
                // Blank entries are silently ignored.
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
                                newSubtaskTitle = ""  // clear the field after adding
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

// =============================================================================
// SPLASH SCREEN
// Displayed for 3 seconds when the app launches before transitioning to the
// Priority screen. Kept intentionally minimal — just the app name and tagline.
// =============================================================================
@Composable
fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text     = "Anchor",
                    color    = MaterialTheme.colorScheme.primary,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text     = "Anchor what matters today",
                    color    = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// =============================================================================
// CREATE / EDIT TASK SCREEN
// Serves double duty: creating a new task (taskId == null) and editing an
// existing one (taskId != null, fields pre-filled from the existing task).
//
// FIELDS:
//   - Title (required — Save/Update is blocked if blank)
//   - Due date and time (optional — two buttons open Material 3 pickers)
//   - Priority (radio: High / Medium / Low; defaults to settings.defaultPriority)
//   - Timeframe (radio: Daily / Weekly / Monthly / Yearly; defaults to settings.defaultTimeframe)
//   - Subtask list (added before saving so no second step is needed after creation)
//
// SUBTASKS:
//   subtaskList is a mutableStateListOf so adding/removing items triggers recomposition.
//   When editing, it is pre-populated with the existing task's subtasks.
//   The full list is passed to addTask()/updateTask() and persisted in a single Room write.
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    taskId: String?,                    // null = creating a new task
    settingsViewModel: SettingsViewModel
) {
    val isEditing  = taskId != null
    val taskToEdit = if (isEditing) taskViewModel.getTaskById(taskId!!) else null
    val settings  by settingsViewModel.settings.collectAsState()

    // Pre-fill fields from the existing task when editing; use defaults for new tasks.
    var titleInput       by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var dueDateMillis    by remember { mutableStateOf(taskToEdit?.dueDateMillis) }
    // "None" listed first so users are not nudged toward any priority tier.
    // Default is "None" (from settings) so new tasks don't get a priority forced on them.
    val priorityOptions   = listOf("None", "High", "Medium", "Low")
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: settings.defaultPriority) }
    // "None" lets users save a task without committing to a timeframe section.
    val timeframeOptions  = listOf("None", "Daily", "Weekly", "Monthly", "Yearly")
    var selectedTimeframe by remember { mutableStateOf(taskToEdit?.timeframe ?: settings.defaultTimeframe) }
    var validationError  by remember { mutableStateOf<String?>(null) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }

    // Subtask list — observable so adding/removing items triggers recomposition.
    // Pre-populated from the existing task when editing; empty for new tasks.
    val subtaskList = remember {
        mutableStateListOf<Subtask>().also { list ->
            taskToEdit?.subtasks?.let { list.addAll(it) }
        }
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    // Material 3 date/time pickers — initialised to the task's existing date when editing.
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // --- Title ---
            Text(text = "Task Title")
            OutlinedTextField(
                value         = titleInput,
                onValueChange = { titleInput = it },
                label         = { Text("Title") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- Due Date & Time ---
            // Two separate pickers: date first, then time. The selected date and time are
            // combined into a single dueDateMillis value in local time.
            Text(text = "Due Date")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showDatePicker = true },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = Color.White
                    )
                ) {
                    // Button label shows the selected date or a placeholder
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

            // Date picker dialog — converts the UTC-based picker result to local time
            // before storing it, because Material 3's DatePicker uses UTC midnight.
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { selectedDate ->
                                    // Picker returns UTC midnight; convert to local calendar date
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

            // Time picker dialog — updates the hour and minute on the existing dueDateMillis
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

            // --- Priority ---
            Text(text = "Priority", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text  = "Choose None to leave priority unassigned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                priorityOptions.forEach { option ->
                    // The entire Row is selectable, not just the RadioButton,
                    // so users can tap anywhere on the row to select the option.
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

            // --- Timeframe ---
            // Displayed in a 2-column grid (chunked into rows of 2) to save vertical space.
            Text(text = "Timeframe", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text  = "Choose None to leave timeframe unassigned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
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

            Spacer(modifier = Modifier.height(24.dp))

            // --- Subtasks ---
            // Users can build a checklist before saving the task.
            // All subtasks are persisted in a single Room write with the parent task,
            // so there is no need to open the task card afterward to add them.
            Text(text = "Subtasks", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Existing pending subtasks — each has a bullet and a remove (X) button
            subtaskList.forEach { subtask ->
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = "•",
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        color    = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text     = subtask.title,
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodyMedium
                    )
                    // X removes the subtask from the in-memory list before saving
                    IconButton(
                        onClick  = { subtaskList.remove(subtask) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove subtask",
                            modifier           = Modifier.size(16.dp),
                            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // "Add subtask" input row — keyboard Done or + button appends to the list
            Row(
                modifier          = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value           = newSubtaskTitle,
                    onValueChange   = { newSubtaskTitle = it },
                    placeholder     = { Text("Add a subtask...") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newSubtaskTitle.isNotBlank()) {
                            subtaskList.add(Subtask(title = newSubtaskTitle.trim()))
                            newSubtaskTitle = ""
                        }
                    })
                )
                IconButton(
                    onClick = {
                        if (newSubtaskTitle.isNotBlank()) {
                            subtaskList.add(Subtask(title = newSubtaskTitle.trim()))
                            newSubtaskTitle = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add subtask")
                }
            }

            // Space above the Save button
            Spacer(modifier = Modifier.height(24.dp))

            // Save / Update — validates title then writes to Room.
            // Returns false (and shows an error dialog) if the title is blank.
            Button(
                onClick = {
                    val success = if (isEditing) {
                        taskViewModel.updateTask(taskId!!, titleInput, dueDateMillis, selectedPriority, selectedTimeframe, subtaskList.toList())
                    } else {
                        taskViewModel.addTask(titleInput, dueDateMillis, selectedPriority, selectedTimeframe, subtaskList.toList())
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

        // Validation error dialog — shown when the user tries to save with a blank title
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

// =============================================================================
// TIME PICKER DIALOG
// Wraps Material 3's TimePicker in an AlertDialog because M3 does not provide
// a built-in TimePickerDialog (unlike DatePickerDialog). onConfirm receives the
// selected hour (0–23) and minute (0–59).
// =============================================================================
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

// =============================================================================
// BOTTOM NAVIGATION BAR
// Four-tab nav bar: Tasks, Priority, Badges, Settings.
// popUpTo(startDestinationId) + launchSingleTop prevents building up a back stack
// when the user taps the same tab multiple times or switches between tabs.
// =============================================================================
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