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
import java.util.UUID


data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val day: String,
    val month: String,
    val year: String,
    val priority: String,
    val timeframe: String,
    val isCompleted: Boolean = false
) {
    val dueDate: String
        get() = if (day.isNotBlank() && month.isNotBlank() && year.isNotBlank()) {
            "$month $day, $year"
        } else {
            ""
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
                if (task.dueDate.isNotBlank()) {
                    Text(text = "Due: ${task.dueDate}", style = MaterialTheme.typography.bodySmall)
                }
            }
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
    val taskToEdit = if (isEditing) taskViewModel.getTaskById(taskId!!) else null

    var titleInput by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var day by remember { mutableStateOf(taskToEdit?.day ?: "") }
    var month by remember { mutableStateOf(taskToEdit?.month ?: "") }
    var year by remember { mutableStateOf(taskToEdit?.year ?: "") }
    val priorityOptions = listOf("High", "Medium", "Low")
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: priorityOptions[1]) }
    val timeframeOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
    var selectedTimeframe by remember { mutableStateOf(taskToEdit?.timeframe ?: timeframeOptions[0]) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("DD") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("MM") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("YYYY") }, modifier = Modifier.weight(1.5f))
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
                        taskViewModel.updateTask(taskId!!, titleInput, day, month, year, selectedPriority, selectedTimeframe)
                    } else {
                        taskViewModel.addTask(titleInput, day, month, year, selectedPriority, selectedTimeframe)
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