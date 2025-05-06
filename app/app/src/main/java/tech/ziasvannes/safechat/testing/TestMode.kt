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
     * Sets all test mode configuration flags for the application.
     *
     * Updates the flags controlling the use of test repositories, simulation of incoming messages, and simulation of connection issues. Each parameter defaults to the initial value of its corresponding flag.
     *
     * @param useTestRepositories If true, enables fake repositories with test data; if false, uses real backend repositories.
     * @param simulateIncomingMessages If true, enables simulation of incoming messages (effective only when using test repositories).
     * @param simulateConnectionIssues If true, enables simulation of connection issues (effective only when using test repositories).
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