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
            startDestination = "tasks_screen",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("tasks_screen") { TasksScreen(navController, taskViewModel) }
            composable("priority_screen") { PriorityScreen(navController = navController, viewModel = taskViewModel) }
            composable("badges_screen") { edu.fullsail.anchor.engagement.badges.BadgesScreen() }
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
    taskViewModel: TaskViewModel
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val groupedTasks = remember(tasks) { tasks.groupBy { it.timeframe } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                item(key = "header_$timeframe") {
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
                        onToggleComplete = { taskViewModel.toggleTaskCompletion(task.id) }
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
        color = MaterialTheme.colorScheme.background
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
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your productivity companion",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

//--- TASK ITEM COMPOSABLE ---
@Composable
fun TaskItem(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                if (task.dueDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.dueDate,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Priority: ${task.priority}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

//--- CREATE TASK SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    taskId: String? = null
) {
    val isEditing = taskId != null
    val existingTask = taskId?.let { taskViewModel.getTaskById(it) }

    var titleInput by remember { mutableStateOf(existingTask?.title ?: "") }
    var dueDateMillis by remember { mutableStateOf(existingTask?.dueDateMillis) }
    var selectedPriority by remember { mutableStateOf(existingTask?.priority ?: "High") }
    var selectedTimeframe by remember { mutableStateOf(existingTask?.timeframe ?: "Daily") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    val priorityOptions = listOf("High", "Medium", "Low",)
    val timeframeOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")

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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(text = "Title", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter task title") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Due Date (Optional)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (dueDateMillis != null) {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(dueDateMillis!!))
                        } else {
                            "Pick Date"
                        }
                    )
                }

                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    enabled = dueDateMillis != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (dueDateMillis != null) {
                            SimpleDateFormat("h:mm a", Locale.getDefault())
                                .format(Date(dueDateMillis!!))
                        } else {
                            "Pick Time"
                        }
                    )
                }
            }

            if (dueDateMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { dueDateMillis = null }) {
                    Text("Clear Due Date")
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val calendar = Calendar.getInstance().apply {
                                        timeInMillis = millis
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                    }
                                    dueDateMillis = calendar.timeInMillis
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
                    onConfirm = { hour: Int, minute: Int ->
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
                        taskViewModel.updateTask(taskId, titleInput, dueDateMillis, selectedPriority, selectedTimeframe)
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
    onConfirm: (Int, Int) -> Unit,
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