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
    
    /**
     * Returns the navigation route string for the chat screen with the specified contact ID.
     *
     * @param contactId The unique identifier of the contact to include in the chat route.
     * @return The formatted chat route string for navigation.
     */
    fun createChatRoute(contactId: String): String {
        return "chat/$contactId"
    }
}