package tech.ziasvannes.safechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import tech.ziasvannes.safechat.testing.TestInitializer
import tech.ziasvannes.safechat.testing.TestMode
import javax.inject.Inject

@HiltAndroidApp
class SafeChatApplication : Application() {
    
    @Inject
    lateinit var testInitializer: TestInitializer
    
    /**
     * Initializes the application and configures test mode settings on startup.
     *
     * Sets up the application to use test repositories by default, disables simulation of incoming messages and connection issues, and initializes test components if required.
     */
    override fun onCreate() {
        super.onCreate()
        
        // By default, enable test repositories for UI development
        // This can be toggled at runtime through the TestSettingsScreen
        TestMode.configure(
            useTestRepositories = true,
            simulateIncomingMessages = false,
            simulateConnectionIssues = false
        )
        
        // Initialize test components if needed
        initializeTestComponents()
    }
    
    /**
     * Initializes test components if test repositories are enabled in test mode.
     *
     * Attempts to initialize test-specific components during application startup. Logs an error if initialization fails.
     */
    private fun initializeTestComponents() {
        if (TestMode.useTestRepositories) {
            try {
                testInitializer.initialize()
            } catch (e: Exception) {
                Log.e("SafeChatApplication", "Failed to initialize test components", e)
                // Optionally disable test mode on failure
                // TestMode.configure(useTestRepositories = false, ...)
            }
        }
    }
}
