package tech.ziasvannes.safechat.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tech.ziasvannes.safechat.presentation.navigation.NavRoutes
import tech.ziasvannes.safechat.presentation.navigation.SafeChatNavHost

/** Main container for the SafeChat application */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Composable that defines the main UI and navigation structure for the SafeChat app.
 *
 * Sets up a scaffold with a bottom navigation bar for Chats, Profile, and Settings, which is hidden
 * on chat detail and add contact screens. The main content area displays screens based on
 * navigation state.
 */
@Composable
fun SafeChatApp() {
    val navController = rememberNavController()

    // Define the bottom navigation items
    val bottomNavItems =
            listOf(
                    BottomNavItem(
                            route = NavRoutes.CONTACTS,
                            icon = Icons.Default.Person,
                            label = "Contacts"
                    ),
                    BottomNavItem(
                            route = NavRoutes.SETTINGS,
                            icon = Icons.Default.Settings,
                            label = "Settings"
                    )
            )

    // Determine whether to show bottom nav based on current destination
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar =
            when {
                currentDestination?.route?.contains("chat/") == true -> false
                currentDestination?.route == NavRoutes.ADD_CONTACT -> false
                else -> true
            }

    Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected =
                                    currentDestination?.hierarchy?.any { it.route == item.route }
                                            ?: false

                            NavigationBarItem(
                                    icon = {
                                        Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            // Pop up to the start destination of the graph to
                                            // avoid building up a large stack of destinations
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            // Avoid multiple copies of the same destination when
                                            // reselecting the same item
                                            launchSingleTop = true
                                            // Restore state when reselecting a previously selected
                                            // item
                                            restoreState = true
                                        }
                                    }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentDestination?.route == NavRoutes.CONTACTS) {
                    FloatingActionButton(
                        onClick = {
                            // Find the current back stack entry and trigger the add contact action
                            navController.navigate(NavRoutes.ADD_CONTACT)
                        },
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it))
        SafeChatNavHost(navController = navController)
    }
}

/** Data class to represent bottom navigation items */
data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)
