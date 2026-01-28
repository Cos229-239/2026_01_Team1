package edu.fullsail.anchor


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.fullsail.anchor.ui.theme.AnchorTheme
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// --- DATA CLASS ---
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueDateMillis: Long?,
    val priority: String,
    val timeframe: String,
    val isCompleted: Boolean = false
) {
    // Formatter for displaying the date and time
    val displayDate: String
        get() {
            if (dueDateMillis == null) return ""
            // Format for the time
            val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            return formatter.format(dueDateMillis)
        }


    val isDueToday: Boolean
        get() {
            if (dueDateMillis == null || isCompleted) return false
            val today = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
            return today.get(Calendar.YEAR) == dueDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == dueDate.get(Calendar.DAY_OF_YEAR)
        }

    // Checks if the due date/time is in the past.
    val isOverdue: Boolean
        get() {
            if (dueDateMillis == null || isCompleted) return false
            // Compare with the current time
            return dueDateMillis < System.currentTimeMillis()
        }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnchorTheme {
                AppNavigation()
            }
        }
    }
}

//--- NAVIGATION ---
@Composable
fun AppNavigation() {
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

    // --- State for Dialogs ---
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }


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
                groupedTasks.forEach { (timeframe: String, tasksInGroup: List<Task>) ->
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
                            onEdit = {
                                selectedTask = task
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedTask = task
                                showDeleteDialog = true
                            },
                            onToggleComplete = { taskViewModel.toggleTaskCompletion(task.id) }
                        )
                    }
                }
            }
        }
    }

    // --- Show Delete Confirmation Dialog ---
    if (showDeleteDialog && selectedTask != null) {
        ConfirmationDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirmation = { taskViewModel.deleteTask(selectedTask!!.id) },
            dialogTitle = "Delete Task",
            dialogText = "Are you sure you want to delete '${selectedTask!!.title}'?"
        )
    }

    // --- Show Edit Confirmation Dialog ---
    if (showEditDialog && selectedTask != null) {
        ConfirmationDialog(
            onDismissRequest = { showEditDialog = false },
            onConfirmation = { navController.navigate("create_task_screen?taskId=${selectedTask!!.id}") },
            dialogTitle = "Edit Task",
            dialogText = "Are you sure you want to edit '${selectedTask!!.title}'?"
        )
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

                // Add this back to show the due date string
                if (task.displayDate.isNotBlank()) {
                    Text(text = task.displayDate, style = MaterialTheme.typography.bodySmall)
                }

                // Display "Overdue" or "Due Today" message
                if (task.isOverdue) {
                    Text("Overdue", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                } else if (task.isDueToday) {
                    Text("Due today", color = Color(0xFFFFA500), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) // Orange color
                }
            }
            // Move the IconButtons inside the Row
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Task")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
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
    taskId: String?
) {
    val isEditing = taskId != null
    val taskToEdit = if (isEditing) taskViewModel.getTaskById(taskId) else null

    var titleInput by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var selectedDateMillis by remember { mutableStateOf(taskToEdit?.dueDateMillis) }
    val priorityOptions = listOf("High", "Medium", "Low")
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: priorityOptions[1]) }
    val timeframeOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
    var selectedTimeframe by remember { mutableStateOf(taskToEdit?.timeframe ?: timeframeOptions[0]) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // --- Date & Time Picker States ---
    val showDatePicker = remember { mutableStateOf(false) }
    val showTimePicker = remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    if (selectedDateMillis != null) {
        calendar.timeInMillis = selectedDateMillis!!
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)    // The timePickerState is now managed inside the TimePickerDialog, so we remove it from here.
    rememberTimePickerState( // This variable is unused
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    // --- Dialogs ---
    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {// This is the key change: Use a UTC Calendar instance
                        val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = datePickerState.selectedDateMillis!!
                        }
                        val current = Calendar.getInstance().apply {
                            if (selectedDateMillis != null) {
                                timeInMillis = selectedDateMillis!!
                            }
                        }
                        // Set the year, month, and day from the UTC calendar
                        current.set(
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH),
                            selectedDate.get(Calendar.DAY_OF_MONTH)
                        )
                        selectedDateMillis = current.timeInMillis
                        showDatePicker.value = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker.value) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker.value = false },
            // MODIFIED: The onConfirm lambda now receives the state
            onConfirm = { returnedTimePickerState ->
                val current = Calendar.getInstance().apply {
                    if (selectedDateMillis != null) {
                        timeInMillis = selectedDateMillis!!
                    }
                }
                current.set(Calendar.HOUR_OF_DAY, returnedTimePickerState.hour)
                current.set(Calendar.MINUTE, returnedTimePickerState.minute)
                selectedDateMillis = current.timeInMillis
                showTimePicker.value = false
            }
        )
    }

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
            Text(text = "Task Title")
            OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(24.dp))

            // --- Date and Time Pickers ---
            Text(text = "Due Date & Time")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Date Button
                OutlinedButton(
                    onClick = { showDatePicker.value = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.padding(end = 8.dp))
                    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                    val dateText = selectedDateMillis?.let { dateFormatter.format(it) } ?: "Set Date"
                    Text(dateText)
                }
                // Time Button
                OutlinedButton(
                    onClick = { showTimePicker.value = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = "Select Time", modifier = Modifier.padding(end = 8.dp))
                    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                    val timeText = selectedDateMillis?.let { timeFormatter.format(it) } ?: "Set Time"
                    Text(timeText)
                }
            }

            // ... (Priority and Timeframe RadioButtons - code is unchanged)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Priority", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.fillMaxWidth()) {
                priorityOptions.forEach { option ->
                    Row(
                        // Complete the modifier like this:
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedPriority == option),
                                onClick = { selectedPriority = option }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedPriority == option),
                            onClick = null // The Row's onClick handles the logic
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp)
                        )
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
                        taskViewModel.updateTask(taskId, titleInput, selectedDateMillis, selectedPriority, selectedTimeframe)
                    } else {
                        taskViewModel.addTask(titleInput, selectedDateMillis, selectedPriority, selectedTimeframe)
                    }

                    if (result is ValidationResult.Success) {
                        navController.popBackStack()
                    } else if (result is ValidationResult.Error) {
                        validationError = result.message
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
            // Use BasicAlertDialog and wrap content in a Surface
            BasicAlertDialog(onDismissRequest = { validationError = null }) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Invalid Input",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { validationError = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    } // End of Column's parent (Scaffold)
}

// Custom composable for the TimePicker dialog for better state handling
@OptIn(ExperimentalMaterial3Api::class) // <-- ADD THIS ANNOTATION
@Composable
fun TimePickerDialog(
    title: String = "Select Time",onDismissRequest: () -> Unit,
    onConfirm: (TimePickerState) -> Unit,
) {
    val timePickerState = rememberTimePickerState()
    // 1. Use BasicAlertDialog instead
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min), // Move size modifiers here
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 2. Place the Surface inside BasicAlertDialog
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp), // Keep padding on the Column
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(20.dp))
                TimePicker(state = timePickerState)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(timePickerState) }) { Text("OK") }
                }
            }
        }
    }
}

// --- CONFIRMATION DIALOG ---
@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                    onDismissRequest()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismissRequest() }
            ) {
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