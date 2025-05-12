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
     * Constructs the navigation route string for a chat screen using the given chat session ID.
     *
     * @param chatSessionId The unique identifier for the chat session.
     * @return The route string for navigating to the specified chat session.
     */
    fun createChatRoute(chatSessionId: String): String {
        return "chat/$chatSessionId"
    }
}
