package tech.ziasvannes.safechat.testing

/**
 * Configuration class to control test mode settings in the Safe Chat app.
 *
 * This singleton provides flags to control whether the app uses real repositories
 * or test repositories with pre-populated data for UI testing and development.
 */
object TestMode {
    /**
     * When true, the app will use fake repositories with test data.
     * When false, the app will use real repositories that connect to backend services.
     *
     * Set this to true to test the UI without a working backend API.
     */
    var useTestRepositories = true

    /**
     * Controls whether randomly timed incoming messages are simulated.
     * Only effective when [useTestRepositories] is true.
     */
    var simulateIncomingMessages = false

    /**
     * Controls whether connection issues are simulated.
     * Only effective when [useTestRepositories] is true.
     */
    var simulateConnectionIssues = false

    /**
     * Set all test mode flags at once.
     */
    fun configure(
        useTestRepositories: Boolean = true,
        simulateIncomingMessages: Boolean = false,
        simulateConnectionIssues: Boolean = false
    ) {
        this.useTestRepositories = useTestRepositories
        this.simulateIncomingMessages = simulateIncomingMessages
        this.simulateConnectionIssues = simulateConnectionIssues
    }
}