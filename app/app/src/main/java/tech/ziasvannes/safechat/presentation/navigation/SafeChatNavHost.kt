package tech.ziasvannes.safechat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.util.UUID
import tech.ziasvannes.safechat.presentation.chat.ChatScreen
import tech.ziasvannes.safechat.presentation.screens.contacts.AddContactScreen
import tech.ziasvannes.safechat.presentation.screens.contacts.ContactListScreen
import tech.ziasvannes.safechat.presentation.screens.profile.ProfileScreen
import tech.ziasvannes.safechat.presentation.screens.settings.SettingsScreen


/**
 * Sets up the navigation graph for the SafeChat app using Jetpack Compose Navigation.
 *
 * Defines navigation routes for contacts, chat, settings, add contact, and profile screens, and handles navigation events between them.
 *
 * @param navController The navigation controller managing app navigation.
 * @param startDestination The initial route to display; defaults to the contacts screen.
 * @param modifier Modifier for styling or layout adjustments.
 */
@Composable
fun SafeChatNavHost(
        navController: NavHostController,
        startDestination: String = NavRoutes.CONTACTS,
        modifier: Modifier = Modifier
) {
    NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
    ) {
        // Contacts list screen
        composable(route = NavRoutes.CONTACTS) {
            ContactListScreen(
                    onNavigateToChat = { chatSessionId ->
                        android.util.Log.d(
                                "SafeChatNavHost",
                                "Navigating to chat route: ${NavRoutes.createChatRoute(chatSessionId.toString())}"
                        )
                        navController.navigate(NavRoutes.createChatRoute(chatSessionId.toString()))
                    },
                    onNavigateToAddContact = { navController.navigate(NavRoutes.ADD_CONTACT) },
                    onNavigateToMe = { navController.navigate(NavRoutes.PROFILE) },
                    onAddContactClick = {
                        navController.navigate(NavRoutes.ADD_CONTACT)
                    }
            )
        }

        // Chat screen
        composable(
                route = NavRoutes.CHAT,
                arguments = listOf(navArgument("chatSessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatSessionIdString =
                    backStackEntry.arguments?.getString("chatSessionId") ?: return@composable
            val chatSessionId =
                    try {
                        UUID.fromString(chatSessionIdString)
                    } catch (e: IllegalArgumentException) {
                        null
                    } ?: return@composable

            ChatScreen(
                    chatSessionId = chatSessionId,
                    onNavigateBack = { navController.navigateUp() }
            )
        }

        // Settings screen
        composable(route = NavRoutes.SETTINGS) {
            SettingsScreen(
                    onNavigateBack = { navController.navigateUp() }
            )
        }

        // Add Contact screen
        composable(route = NavRoutes.ADD_CONTACT) {
            AddContactScreen(onNavigateBack = { navController.navigateUp() })
        }

        // Profile screen
        composable(route = NavRoutes.PROFILE) {
            ProfileScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}
