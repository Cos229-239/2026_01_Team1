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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.fullsail.anchor.ui.theme.AnchorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDateMillis: Long?,
    val priority: String,
    val timeframe: String,
    val isCompleted: Boolean = false
) {
    val dueDate: String
        get() {
            if (dueDateMillis == null) {
                return ""
            }

            val today = Calendar.getInstance()
            val dueDateCal = Calendar.getInstance()
            dueDateCal.timeInMillis = dueDateMillis

            // Reset time part for accurate day comparison
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val dueDateCalendar = Calendar.getInstance()
            dueDateCalendar.timeInMillis = dueDateMillis
            dueDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dueDateCalendar.set(Calendar.MINUTE, 0)
            dueDateCalendar.set(Calendar.SECOND, 0)
            dueDateCalendar.set(Calendar.MILLISECOND, 0)

            val diff = dueDateCalendar.timeInMillis - today.timeInMillis
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                days == 0L -> "Due today at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueDateMillis))}"
                days == 1L -> "Due tomorrow at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueDateMillis))}"
                days < 0L -> "${-days} days overdue"
                else -> {
                    val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    sdf.format(Date(dueDateMillis))
                }
            }
        }
}

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
                // track which screen is showing
                var showSplash by remember { mutableStateOf(true) }
                // Launch the coroutine(splashscreen) that will switch after 3 seconds
                LaunchedEffect(Unit) {
                    delay(3000) // this is 3000ms or 3 seconds
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                } else {
                    // after displaying splashscreen then switch to main app.
                    Scaffold { innerPadding ->
                        AppNavigation(innerPadding)
                    }
                }
            }
        }
    }
}

//--- NAVIGATION ---
@Composable
fun AppNavigation(innerPadding: PaddingValues) {
    val navController = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "tasks_screen",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("tasks_screen") {
                TasksScreen(
                    navController = navController,
                    taskViewModel = taskViewModel
                )
            }
            composable("badges_screen"){
                edu.fullsail.anchor.engagement.badges.BadgesScreen()
            }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavController,
    taskViewModel: TaskViewModel
) {
    val tasks = taskViewModel.tasks
    val groupedTasks = tasks.groupBy { it.timeframe }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tasks") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            onToggleComplete = { taskViewModel.toggleTaskCompletion(task.id) }
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
                    // hardcoding the font color for now as the Teal we picked. May make a variable to use it later
                    color = Color(0xFF2F9E97),
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Anchor what matters today",
                    // hardcoding Teal color here as well.
                    color = Color(0xFF2F9E97),
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
    var selectedTimeframe by remember { mutableStateOf(taskToEdit?.timeframe ?: timeframeOptions[0]) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = dueDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 0,
        initialMinute = dueDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Task" else "Create Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Due Date")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showDatePicker = true }) {
                    Text(text = dueDateMillis?.let { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select a date")
                }
                Button(onClick = { showTimePicker = true }) {
                    Text(text = dueDateMillis?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it)) } ?: "Select a time")
                }
            }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDate ->
                                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                utcCal.timeInMillis = selectedDate

                                val localCal = Calendar.getInstance()
                                dueDateMillis?.let {
                                    localCal.timeInMillis = it
                                }

                                localCal.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                localCal.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                localCal.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))

                                dueDateMillis = localCal.timeInMillis
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
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

            Button(
                onClick = {
                    val result = if (isEditing) {
                        taskViewModel.updateTask(taskId!!, titleInput, dueDateMillis, selectedPriority, selectedTimeframe)
                    } else {
                        taskViewModel.addTask(titleInput, dueDateMillis, selectedPriority, selectedTimeframe)
                    }

                    when (result) {
                        is ValidationResult.Success -> navController.popBackStack()
                        is ValidationResult.Error -> validationError = result.message
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

//--- BOTTOM NAVIGATION BAR ---
@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Add, contentDescription = "Tasks") },
            label = { Text("Tasks") },
            selected = true,
            onClick = { navController.navigate("tasks_screen") }
        )
        FloatingActionButton(
            onClick = { navController.navigate("create_task_screen") },
            modifier = Modifier.padding(top = 8.dp),
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task")
        }
        /*
        Navigation bar for Badges screen
         */
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Star, contentDescription = "Badges") },
            label = { Text("Badges") },
            selected = false,
            onClick = { navController.navigate("badges_screen")}
        )
    }
}
