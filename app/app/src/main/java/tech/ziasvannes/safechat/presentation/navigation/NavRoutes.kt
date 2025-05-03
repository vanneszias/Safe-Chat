package tech.ziasvannes.safechat.presentation.navigation

/**
 * Navigation routes for the app
 */
object NavRoutes {
    const val CONTACTS = "contacts"
    const val CHAT = "chat/{contactId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ADD_CONTACT = "add_contact"
    
    // Helper function to create chat route with contactId parameter
    fun createChatRoute(contactId: String): String {
        return "chat/$contactId"
    }
}