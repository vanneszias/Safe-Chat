package tech.ziasvannes.safechat.presentation.navigation

/** Navigation routes for the app */
object NavRoutes {
    const val CONTACTS = "contacts"
    const val CHAT = "chat/{chatSessionId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ADD_CONTACT = "add_contact"
    const val TEST_SETTINGS = "test_settings"

    /**
     * Returns the navigation route for a chat screen with the specified chat session ID.
     *
     * @param chatSessionId The unique identifier of the chat session to include in the route.
     * @return The route string for navigating to the chat screen with the given session.
     */
    fun createChatRoute(chatSessionId: String): String {
        return "chat/$chatSessionId"
    }
}
