package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DumboContainerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LanguagesCatalogScreen
import com.example.ui.screens.SauronCameraScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VozLinguaTheme
import com.example.ui.viewmodel.TranscriberViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Dumbo : Screen(
        route = "dumbo",
        title = "Dumbo",
        selectedIcon = Icons.Filled.RecordVoiceOver,
        unselectedIcon = Icons.Outlined.RecordVoiceOver,
        testTag = "nav_dumbo"
    )

    object Sauron : Screen(
        route = "sauron",
        title = "Sauron",
        selectedIcon = Icons.Filled.Visibility,
        unselectedIcon = Icons.Outlined.Visibility,
        testTag = "nav_sauron"
    )

    object History : Screen(
        route = "history",
        title = "Histórico",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history"
    )

    object Languages : Screen(
        route = "languages",
        title = "Línguas",
        selectedIcon = Icons.Filled.Language,
        unselectedIcon = Icons.Outlined.Language,
        testTag = "nav_languages"
    )

    object Settings : Screen(
        route = "settings",
        title = "Definições",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VozLinguaTheme(darkTheme = true) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: TranscriberViewModel = viewModel()
) {
    val navItems = listOf(
        Screen.Dumbo,
        Screen.Sauron,
        Screen.History,
        Screen.Languages,
        Screen.Settings
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SophisticatedBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SophisticatedSurface,
                tonalElevation = 6.dp,
                modifier = Modifier.border(
                    width = 0.5.dp,
                    color = SophisticatedOutline
                )
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag(item.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LavenderPrimary,
                            selectedTextColor = LavenderPrimary,
                            indicatorColor = LavenderContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "tab_transition",
            modifier = Modifier.padding(innerPadding)
        ) { tabIndex ->
            when (tabIndex) {
                0 -> DumboContainerScreen(viewModel = viewModel)
                1 -> SauronCameraScreen(viewModel = viewModel)
                2 -> HistoryScreen(viewModel = viewModel)
                3 -> LanguagesCatalogScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
