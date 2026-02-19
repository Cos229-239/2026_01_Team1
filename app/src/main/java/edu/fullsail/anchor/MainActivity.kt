package edu.fullsail.anchor

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
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
import java.text.SimpleDateFormat
import java.util.*
import edu.fullsail.anchor.engagement.badges.BadgesViewModel
import edu.fullsail.anchor.engagement.badges.BadgeRuleEngine

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                "Light"  -> false
                "Dark"   -> true
                else     -> systemDark  // "System"
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
    Shared BadgesViewModel instance so task completion can trigger badge evaluation
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
                                "tasks_screen" -> "Tasks"
                                "priority_screen" -> "Priority"
                                "badges_screen" -> "Badges"
                                "settings_screen" -> "Settings"  // Title for settings screen
                                else -> ""
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            if (currentRoute in listOf("tasks_screen", "priority_screen")) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_task_screen") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "priority_screen",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pass settingsViewModel to screens that need it
            composable("tasks_screen") { TasksScreen(navController, taskViewModel, badgesViewModel, settingsViewModel) }
            composable("priority_screen") { PriorityScreen(navController, taskViewModel, badgesViewModel, settingsViewModel) }
            composable("badges_screen") { edu.fullsail.anchor.engagement.badges.BadgesScreen(badgesViewModel) }
            // Settings screen route
            composable("settings_screen") { SettingsScreen(settingsViewModel) }
            composable(
                route = "create_task_screen?taskId={taskId}",
                arguments = listOf(navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                CreateTaskScreen(
                    navController = navController,
                    taskViewModel = taskViewModel,
                    taskId = taskId,
                    settingsViewModel = settingsViewModel  // Pass settings for defaults
                )
            }
        }
    }
}

//--- TASKS SCREEN ---
// Added settingsViewModel parameter
@Composable
fun TasksScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    badgesViewModel: BadgesViewModel,
    settingsViewModel: SettingsViewModel  // Receive settings
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val groupedTasks = tasks.groupBy { it.timeframe }
    val settings by settingsViewModel.settings.collectAsState()  // Observe settings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        if (tasks.isEmpty()) {
            item {
                Text(
                    "No tasks yet. Press the '+' button to add one!",
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        } else {
            groupedTasks.forEach { (timeframe, tasksInGroup) ->
                item {
                    Text(
                        text = timeframe,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(tasksInGroup, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                        onDelete = { taskViewModel.deleteTask(task.id) },
                        // Pass settings for compact mode and confirm delete
                        settings = settings,
                        /*
                        BADGE SYSTEM BRIDGE:
                        After a task is completed, we rebuild UserEngagementStats and re-evaluate badges.
                        This keeps badge progress/ unlocks in sync with real task behavior.
                        Do not remove without updating the badge evaluation pipeline.
                         */
                        onToggleComplete = { taskViewModel.toggleTaskCompletion(task.id)
                            val stats = taskViewModel.buildEngagementStats()
                            val (updatedBadges, newlyUnlocked) = BadgeRuleEngine.evaluate(
                                stats = stats,
                                existing = badgesViewModel.badges
                            )
                            badgesViewModel.saveBadges(updatedBadges)
                        }
                    )
                }
            }
        }
    }
}

// --- Splash Screen ---
@Composable
fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // will match light or dark theme if we switch color
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Anchor",
                    // Using Color Palate from Dustin
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Anchor what matters today",
                    // Using Color Palate from Dustin
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

//--- TASK ITEM ---
// Added settings parameter for compact mode and confirm delete
@Composable
fun TaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    settings: AppSettings  // Receive settings
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Apply compact mode - reduce padding when enabled
    val cardPadding = if (settings.compactMode) 6.dp else 12.dp

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(cardPadding),  // Dynamic padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, fontWeight = FontWeight.Bold)
                if (task.dueDate.isNotBlank()) {
                    Text(text = task.dueDate, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Task")
            }
            // Delete button - show dialog or delete immediately based on setting
            IconButton(onClick = {
                if (settings.confirmBeforeDeleting) {
                    showDeleteDialog = true
                } else {
                    onDelete()
                }
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
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

//--- CREATE TASK SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    taskId: String?,
    settingsViewModel: SettingsViewModel  // Receive settings for defaults
) {
    val isEditing = taskId != null
    val taskToEdit = if (isEditing) taskViewModel.getTaskById(taskId!!) else null
    val settings by settingsViewModel.settings.collectAsState() // Observe settings

    var titleInput by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var dueDateMillis by remember { mutableStateOf(taskToEdit?.dueDateMillis) }
    val priorityOptions = listOf("High", "Medium", "Low")
    // Use settings default for new tasks, existing value for editing
    var selectedPriority by remember {
        mutableStateOf(taskToEdit?.priority ?: settings.defaultPriority)
    }
    val timeframeOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
    // Use settings default for new tasks, existing value for editing
    var selectedTimeframe by remember {
        mutableStateOf(taskToEdit?.timeframe ?: settings.defaultTimeframe)
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
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
            // --- All input fields ---
            Text(text = "Task Title")
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Due Date")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // --------- DATE BUTTON --------
                Button(
                    onClick = { showDatePicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = dueDateMillis?.let {
                        SimpleDateFormat(
                            "MMMM dd, yyyy",
                            Locale.getDefault()
                        ).format(Date(it))
                    } ?: "Select a date")
                }
                // --------- TIME BUTTON ----------
                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = dueDateMillis?.let {
                        SimpleDateFormat(
                            "h:mm a",
                            Locale.getDefault()
                        ).format(Date(it))
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
                                    dueDateMillis?.let {
                                        localCal.timeInMillis = it
                                    }

                                    localCal.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                    localCal.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                    localCal.set(
                                        Calendar.DAY_OF_MONTH,
                                        utcCal.get(Calendar.DAY_OF_MONTH)
                                    )

                                    dueDateMillis = localCal.timeInMillis
                                }
                                showDatePicker = false
                            },
                            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                "OK",
                                color = Color.White
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDatePicker = false },
                            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                "Cancel",
                                color = Color.White
                            )
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
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
                            .selectable(
                                selected = (selectedPriority == option),
                                onClick = { selectedPriority = option })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (selectedPriority == option), onClick = null)
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
                                    .selectable(
                                        selected = (selectedTimeframe == option),
                                        onClick = { selectedTimeframe = option }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedTimeframe == option),
                                    onClick = null
                                )
                                Text(
                                    text = option,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(if (isEditing) "Update Task" else "Save Task", fontSize = 16.sp)
            }
        }

        if (validationError != null) {
            AlertDialog(
                onDismissRequest = { validationError = null },
                title = { Text("Invalid Input") },
                text = { Text(validationError!!) },
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
        title = { Text("Select Time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.hour, state.minute) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cancel")
            }
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
            icon = { Icon(Icons.Filled.List, contentDescription = "Tasks") },
            label = { Text("Tasks") },
            selected = currentRoute == "tasks_screen",
            onClick = {
                navController.navigate("tasks_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Flag, contentDescription = "Priority") },
            label = { Text("Priority") },
            selected = currentRoute == "priority_screen",
            onClick = {
                navController.navigate("priority_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Star, contentDescription = "Badges") },
            label = { Text("Badges") },
            selected = currentRoute == "badges_screen",
            onClick = {
                navController.navigate("badges_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
        // Settings Screen
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings_screen",
            onClick = {
                navController.navigate("settings_screen") {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
    }
}