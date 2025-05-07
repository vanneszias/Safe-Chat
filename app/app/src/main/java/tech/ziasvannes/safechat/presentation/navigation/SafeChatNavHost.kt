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
import tech.ziasvannes.safechat.presentation.screens.settings.TestSettingsScreen

/**
 * Navigation host for the SafeChat application
 *
 * @param navController The navigation controller
 * @param startDestination The starting destination route
 * @param modifier Optional modifier for styling
 */
/**
 * Defines the navigation graph for the SafeChat app using Jetpack Compose Navigation.
 *
 * Sets up navigation routes for contacts, chat, profile, settings, and add contact screens,
 * handling argument parsing and back navigation as needed.
 *
 * @param startDestination The initial route to display when the navigation host is created.
 * Defaults to the contacts list.
 * @param modifier Optional modifier for customizing the navigation host's layout or appearance.
 */
/**
 * Sets up the navigation graph for the SafeChat app using Jetpack Compose Navigation.
 *
 * Defines routes for contacts, chat, profile, settings, test settings, and add contact screens, and
 * manages navigation between them.
 *
 * @param navController The navigation controller that manages app navigation.
 * @param startDestination The initial route to display; defaults to the contacts list.
 * @param modifier Optional modifier for the NavHost.
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
                        navController.navigate(NavRoutes.createChatRoute(chatSessionId.toString()))
                    },
                    onNavigateToAddContact = { navController.navigate(NavRoutes.ADD_CONTACT) }
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

        // Profile screen
        composable(route = NavRoutes.PROFILE) {
            ProfileScreen(onNavigateBack = { navController.navigateUp() })
        }

        // Settings screen
        composable(route = NavRoutes.SETTINGS) {
            SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToTestSettings = { navController.navigate(NavRoutes.TEST_SETTINGS) }
            )
        }

        // Test Settings screen
        composable(route = NavRoutes.TEST_SETTINGS) {
            TestSettingsScreen(onNavigateBack = { navController.navigateUp() })
        }

        // Add Contact screen
        composable(route = NavRoutes.ADD_CONTACT) {
            AddContactScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}
