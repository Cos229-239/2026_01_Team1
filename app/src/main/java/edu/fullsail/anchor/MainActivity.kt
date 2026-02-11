package edu.fullsail.anchor

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
import edu.fullsail.anchor.ui.theme.AnchorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import edu.fullsail.anchor.engagement.badges.BadgesViewModel
import edu.fullsail.anchor.engagement.badges.BadgeRuleEngine
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !isDarkTheme
            }
            AnchorTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(3000) // 3 seconds
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                } else {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    /*
    Shared BadgesViewModel instance so task completion can trigger badge evaluation
    Note (Gamification): Keep this BadgesViewModel created at the navigation level.
    Task completion uses this shared instance to eval/update badges.
    Removing / Moving this line will break tasks -> badge progress / unlocks.
    */
    val badgesViewModel: BadgesViewModel = viewModel()

    Scaffold(
        topBar = {
            if (currentRoute in listOf("tasks_screen", "priority_screen", "badges_screen")) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentRoute) {
                                "tasks_screen" -> "Tasks"
                                "priority_screen" -> "Priority"
                                "badges_screen" -> "Badges"
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
            composable("tasks_screen") { TasksScreen(navController, taskViewModel, badgesViewModel) }
            composable("priority_screen") { PriorityScreen(navController, taskViewModel, badgesViewModel) }
            composable("badges_screen") { edu.fullsail.anchor.engagement.badges.BadgesScreen(badgesViewModel) }
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
                    taskId = taskId
                )
            }
        }
    }
}

//--- TASKS SCREEN ---
@Composable
fun TasksScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    badgesViewModel: BadgesViewModel
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val groupedTasks = tasks.groupBy { it.timeframe }

    // Track expanded state for each timeframe section
    // NOTE: Changed from rememberSaveable to remember to fix crash (The app would crash after pressing task list)
    // Using remember instead of rememberSaveable because mutableStateMapOf is not directly serializable
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Initialize all sections as expanded by default
    // NOTE: Added for collapsible sections feature - ensures all sections start expanded
    LaunchedEffect(groupedTasks.keys) {
        groupedTasks.keys.forEach { timeframe ->
            if (!expandedSections.containsKey(timeframe)) {
                expandedSections[timeframe] = true
            }
        }
    }

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
                // Replaced Text header with CollapsibleHeader component
                item(key = "header_$timeframe") {
                    CollapsibleHeader(
                        title = timeframe,
                        isExpanded = expandedSections[timeframe] ?: true,
                        onToggle = {
                            expandedSections[timeframe] = !(expandedSections[timeframe] ?: true)
                        },
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                // Only render task items if section is expanded
                // NOTE: This conditional prevents rendering items in LazyColumn when collapsed
                if (expandedSections[timeframe] == true) {
                    items(tasksInGroup, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onEdit = { navController.navigate("create_task_screen?taskId=${task.id}") },
                            onDelete = { taskViewModel.deleteTask(task.id) },
                            /*
                            BADGE SYSTEM BRIDGE:
                            After a task is completed, we rebuild UserEngagementStats and re-evaluate badges.
                            This keeps badge progress/ unlocks in sync with real task behavior.
                            Do not remove without updating the badge evaluation pipeline.
                             */
                            onToggleComplete = {
                                taskViewModel.toggleTaskCompletion(task.id)
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
@Composable
fun TaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
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
            IconButton(onClick = { showDeleteDialog = true }) {
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
    taskId: String?
) {
    val isEditing = taskId != null
    val taskToEdit = if (isEditing) taskViewModel.getTaskById(taskId!!) else null

    var titleInput by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var dueDateMillis by remember { mutableStateOf(taskToEdit?.dueDateMillis) }
    val priorityOptions = listOf("High", "Medium", "Low")
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: priorityOptions[1]) }
    val timeframeOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
    var selectedTimeframe by remember {
        mutableStateOf(
            taskToEdit?.timeframe ?: timeframeOptions[0]
        )
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
    }
}

@Composable
fun CollapsibleHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron rotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )

        if (trailingText != null) {
            Text(
                text = trailingText,
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