package com.example.fitpick

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitpick.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FitPickTheme { GradientBackground { FitPickApp() } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitPickApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val context = LocalContext.current

    LaunchedEffect(vm.message) {
        vm.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.message = null
        }
    }

    val tabs = listOf(
        Triple("home", "Home", Icons.Filled.Home),
        Triple("history", "Progress", Icons.Filled.DateRange),
        Triple("settings", "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (route in tabs.map { it.first }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                    tabs.forEach { (r, label, icon) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = {
                                if (r == "home") {
                                    nav.popBackStack("home", false)
                                } else {
                                    nav.navigate(r) {
                                        popUpTo("home")
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(vm, onCreate = { nav.navigate("create") }, onOpen = { nav.navigate("routine") })
            }
            composable("create") {
                CreateScreen(vm, onBack = { nav.popBackStack() }, onDone = { nav.navigate("routine") })
            }
            composable("routine") {
                RoutineScreen(vm, onBack = { nav.popBackStack() }, onStart = { nav.navigate("workout") })
            }
            composable("workout") {
                WorkoutScreen(
                    vm,
                    onBack = { nav.popBackStack() },
                    onHome = { nav.popBackStack("home", false) },
                    onProgress = { nav.navigate("history") { popUpTo("home") } }
                )
            }
            composable("history") { HistoryScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}
