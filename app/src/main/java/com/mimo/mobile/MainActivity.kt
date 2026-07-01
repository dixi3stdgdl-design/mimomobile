package com.mimo.mobile

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mimo.mobile.network.ConnectionState
import com.mimo.mobile.ui.theme.*
import com.mimo.mobile.ui.screens.*
import com.mimo.mobile.ui.components.PerspectiveGridBackground
import com.mimo.mobile.viewmodel.MiMoViewModel
import kotlinx.coroutines.delay

sealed class Screen(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Chat : Screen("Chat", Icons.Filled.ChatBubbleOutline, Icons.Filled.Chat)
    data object Build : Screen("Build", Icons.Filled.AccountTree, Icons.Filled.AccountTree)
    data object Files : Screen("Files", Icons.Filled.FolderOpen, Icons.Filled.Folder)
    data object Terminal : Screen("Terminal", Icons.Filled.Terminal, Icons.Filled.Terminal)
    data object Remote : Screen("Remote", Icons.Filled.DesktopWindows, Icons.Filled.DesktopWindows)
    data object Settings : Screen("Settings", Icons.Filled.Settings, Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        ObjectAnimator.ofFloat(window.decorView, View.ALPHA, 1f).apply {
            duration = 500L
            interpolator = DecelerateInterpolator()
            start()
        }

        setContent {
            val vm: MiMoViewModel = viewModel()
            val state by vm.state.collectAsState()

            MiMoTheme(userDarkMode = state.darkMode) {
                LaunchedEffect(Unit) {
                    delay(1500L)
                    keepSplash = false
                    vm.dismissSplash()
                }

                if (state.isSplashDone) {
                    MiMoApp(vm, state)
                } else {
                    SplashContent()
                }
            }
        }
    }
}

@Composable
fun SplashContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(80.dp).scale(scale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "M",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "MiMo Mobile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your AI, everywhere",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiMoApp(vm: MiMoViewModel, state: com.mimo.mobile.viewmodel.AppState) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }

    val navScreens = listOf(Screen.Chat, Screen.Build, Screen.Files, Screen.Terminal, Screen.Remote, Screen.Settings)

    Box(modifier = Modifier.fillMaxSize()) {
        PerspectiveGridBackground()

        Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("MiMo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                },
                actions = {
                    if (state.connectionState == ConnectionState.CONNECTED) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("ON", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                navScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                if (currentScreen == screen) screen.selectedIcon else screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = {
                        val targetIndex = navScreens.indexOf(targetState)
                        val sourceIndex = navScreens.indexOf(initialState)
                        if (targetIndex > sourceIndex) it / 3 else -it / 3
                    }
                ) togetherWith fadeOut(animationSpec = tween(200))
            },
            modifier = Modifier.padding(paddingValues),
            label = "screen"
        ) { screen ->
            when (screen) {
                Screen.Chat -> ChatScreen(vm)
                Screen.Build -> BuildVisualizerScreen(vm)
                Screen.Files -> FileBrowserScreen(vm)
                Screen.Terminal -> TerminalScreen(vm)
                Screen.Remote -> RemoteScreen(vm)
                Screen.Settings -> SettingsScreen(
                    host = state.serverHost,
                    port = state.serverPort,
                    onHostChange = vm::updateHost,
                    onPortChange = vm::updatePort,
                    onReconnect = vm::reconnect,
                    connectionState = state.connectionState,
                    vm = vm
                )
            }
        }
    }
    }
}
