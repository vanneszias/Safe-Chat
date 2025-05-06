package tech.ziasvannes.safechat.testing

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Initializes all test components for the Safe Chat app.
 *
 * This class ensures that test data and simulations are properly
 * set up when the app starts in test mode.
 */
class TestInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository
) {
    
    private var messageSimulator: TestMessageSimulator? = null
    
    /**
     * Sets up test components for the application if test repositories are enabled.
     *
     * Creates and initializes a test message simulator when running in test mode, ensuring its state reflects current test settings.
     */
    fun initialize() {
        Log.d(TAG, "Initializing test components, useTestRepositories=${TestMode.useTestRepositories}")
        
        // Initialize message simulator if needed
        if (messageSimulator == null && TestMode.useTestRepositories) {
            messageSimulator = TestMessageSimulator(
                context = context,
                contactRepository = contactRepository,
                messageRepository = messageRepository
            )
            
            // Update the simulator state based on current settings
            messageSimulator?.updateSimulationState()
        }
    }
    
    /**
     * Refreshes the test message simulator's state to reflect current test settings.
     *
     * Invokes an update on the simulator if it has been initialized, ensuring simulation behavior matches the latest configuration.
     */
    fun updateTestState() {
        messageSimulator?.updateSimulationState()
    }
    
    companion object {
        private const val TAG = "TestInitializer"
    }
}