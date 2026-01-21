package edu.fullsail.anchor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.fullsail.anchor.ui.theme.AnchorTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

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
                    Scaffold { innerPadding ->
                        MainScreen(innerPadding)
                    }
                }
            }
        }
    }
}

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
                    // hardcoding the font color for now. May make a variable to use it later
                    color = Color(0xFF2F9E97),
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Anchor what matters today",
                    color = Color(0xFF2F9E97),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun MainScreen(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Text(
            text = "This is the main screen!",
            // this is so the text on main screen will match light or dark theme
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp
        )
    }
}