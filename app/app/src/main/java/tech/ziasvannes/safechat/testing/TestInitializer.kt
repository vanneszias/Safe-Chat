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
     * Initializes all test components based on the current TestMode settings.
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
     * Updates the test component state when test settings change.
     */
    fun updateTestState() {
        messageSimulator?.updateSimulationState()
    }
    
    companion object {
        private const val TAG = "TestInitializer"
    }
}