package tech.ziasvannes.safechat

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import tech.ziasvannes.safechat.testing.TestInitializer
import tech.ziasvannes.safechat.testing.TestMode

@HiltAndroidApp
class SafeChatApplication : Application() {

    @Inject lateinit var testInitializer: TestInitializer

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

    /** Initializes test components for development and testing. */
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
