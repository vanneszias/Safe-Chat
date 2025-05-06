package tech.ziasvannes.safechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import tech.ziasvannes.safechat.testing.TestInitializer
import tech.ziasvannes.safechat.testing.TestMode
import javax.inject.Inject

@HiltAndroidApp
class SafeChatApplication : Application() {
    
    @Inject
    lateinit var testInitializer: TestInitializer
    
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
     * Initializes test components for development and testing.
     */
    private fun initializeTestComponents() {
        if (TestMode.useTestRepositories) {
            testInitializer.initialize()
        }
    }
}