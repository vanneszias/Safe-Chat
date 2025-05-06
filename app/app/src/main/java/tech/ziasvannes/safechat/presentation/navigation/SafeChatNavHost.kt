package tech.ziasvannes.safechat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.ziasvannes.safechat.presentation.chat.ChatScreen
import tech.ziasvannes.safechat.presentation.screens.contacts.ContactListScreen
import tech.ziasvannes.safechat.presentation.screens.profile.ProfileScreen
import tech.ziasvannes.safechat.presentation.screens.settings.SettingsScreen
import tech.ziasvannes.safechat.presentation.screens.settings.TestSettingsScreen
import java.util.UUID

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
 * Sets up navigation routes for contacts, chat, profile, settings, and add contact screens, handling argument parsing and back navigation as needed.
 *
 * @param startDestination The initial route to display when the navigation host is created. Defaults to the contacts list.
 * @param modifier Optional modifier for customizing the navigation host's layout or appearance.
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
                onNavigateToChat = { contactId ->
                    navController.navigate(NavRoutes.createChatRoute(contactId.toString()))
                },
                onNavigateToAddContact = {
                    navController.navigate(NavRoutes.ADD_CONTACT)
                }
            )
        }
        
        // Chat screen
        composable(
            route = NavRoutes.CHAT,
            arguments = listOf(
                navArgument("contactId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactIdString = backStackEntry.arguments?.getString("contactId") ?: return@composable
            val contactId = try {
                UUID.fromString(contactIdString)
            } catch (e: IllegalArgumentException) {
                null
            } ?: return@composable
            
            ChatScreen(
                contactId = contactId,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // Profile screen
        composable(route = NavRoutes.PROFILE) {
            ProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // Settings screen
        composable(route = NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToTestSettings = {
                    navController.navigate(NavRoutes.TEST_SETTINGS)
                }
            )
        }
        
        // Test Settings screen
        composable(route = NavRoutes.TEST_SETTINGS) {
            TestSettingsScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // Add Contact screen - would be implemented in future
        composable(route = NavRoutes.ADD_CONTACT) {
            // Temporary placeholder for add contact screen
            ProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}