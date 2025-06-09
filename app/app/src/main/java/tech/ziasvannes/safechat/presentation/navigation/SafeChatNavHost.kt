package tech.ziasvannes.safechat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.util.UUID
import tech.ziasvannes.safechat.presentation.chat.ChatScreen
import tech.ziasvannes.safechat.presentation.screens.contacts.AddContactScreen
import tech.ziasvannes.safechat.presentation.screens.contacts.ContactListScreen
import tech.ziasvannes.safechat.presentation.screens.profile.AuthScreen
import tech.ziasvannes.safechat.presentation.screens.profile.ProfileScreen
import tech.ziasvannes.safechat.presentation.screens.settings.SettingsScreen
import tech.ziasvannes.safechat.session.UserSession
import javax.inject.Inject

@Composable
fun SafeChatNavHost(
        navController: NavHostController,
        modifier: Modifier = Modifier,
        userSession: UserSession = hiltViewModel<SessionViewModel>().userSession
) {
    val startDestination = if (userSession.isLoggedIn()) NavRoutes.CONTACTS else NavRoutes.AUTH
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
                    onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) }
            )
        }

        // Add Contact screen
        composable(route = NavRoutes.ADD_CONTACT) {
            AddContactScreen(onNavigateBack = { navController.navigateUp() })
        }

        // Settings screen
        composable(route = NavRoutes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }

        // Auth screen
        composable(route = NavRoutes.AUTH) {
            AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(NavRoutes.CONTACTS) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    }
            )
        }

        // Profile screen (for direct navigation from contacts screen only)
        composable(route = NavRoutes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onLogout = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Chat screen
        composable(
                route = NavRoutes.CHAT,
                arguments = listOf(navArgument("chatSessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatSessionIdArg = backStackEntry.arguments?.getString("chatSessionId")
            val chatSessionId =
                    chatSessionIdArg?.let {
                        runCatching { java.util.UUID.fromString(it) }.getOrNull()
                    }
            ChatScreen(
                    chatSessionId = chatSessionId,
                    onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
