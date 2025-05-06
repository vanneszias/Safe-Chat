package tech.ziasvannes.safechat.testing

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.ziasvannes.safechat.domain.repository.ContactRepository
import tech.ziasvannes.safechat.domain.repository.MessageRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulates incoming messages for testing the UI when no real API connection is available.
 *
 * This class periodically sends simulated messages from random contacts when test mode is enabled.
 */
@Singleton
class TestMessageSimulator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository
) : DefaultLifecycleObserver {

    private val simulatorScope = CoroutineScope(Dispatchers.IO)
    private var simulationJob: Job? = null

    // Sample message templates to make conversations seem more realistic
    private val messageTemplates = listOf(
        "Hey there!",
        "How's it going?",
        "Are we still meeting today?",
        "Did you see that new announcement?",
        "I just sent you the files",
        "Can you call me when you get a chance?",
        "Don't forget about tomorrow's meeting",
        "What time works for you?",
        "Thanks for your help!",
        "Let me know what you think",
        "I'll get back to you soon",
        "Sorry for the late response",
        "Check out this link: https://example.com",
        "Have you tried the new feature?",
        "Looking forward to catching up"
    )

    init {
        // Register as a lifecycle observer to handle app lifecycle changes
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Initiates message simulation when the app enters the foreground, if test mode and message simulation are enabled.
     *
     * @param owner The lifecycle owner triggering the start event.
     */
    override fun onStart(owner: LifecycleOwner) {
        if (TestMode.useTestRepositories && TestMode.simulateIncomingMessages) {
            startSimulation()
        }
    }

    /**
     * Stops message simulation when the application moves to the background.
     *
     * Called automatically as part of the app's lifecycle.
     */
    override fun onStop(owner: LifecycleOwner) {
        stopSimulation()
    }

    /**
     * Enables or disables simulated incoming messages according to current test mode flags.
     *
     * Starts the simulation if both test repositories and message simulation are enabled; otherwise, stops any ongoing simulation.
     */
    fun updateSimulationState() {
        if (TestMode.useTestRepositories && TestMode.simulateIncomingMessages) {
            if (simulationJob == null || simulationJob?.isActive != true) {
                startSimulation()
            }
        } else {
            stopSimulation()
        }
    }

    /**
     * Begins a background coroutine that periodically simulates incoming messages from random contacts for UI testing.
     *
     * Cancels any existing simulation before starting. The simulation continues while test mode flags are enabled, sending random messages from a predefined template to random contacts at random intervals between 30 seconds and 2 minutes. Only operates if the message repository is a `FakeMessageRepository`. Exceptions during simulation are caught and logged.
     */
    private fun startSimulation() {
        // Stop any existing simulation
        stopSimulation()

        Log.d(TAG, "Starting message simulation")

        simulationJob = simulatorScope.launch {
            while (isActive) {
                try {
                    if (!TestMode.simulateIncomingMessages || !TestMode.useTestRepositories) {
                        break
                    }

                    // Get all contacts
                    val contacts = getContacts()
                    if (contacts.isNotEmpty()) {
                        // Randomly select a contact to send a message
                        val randomContact = contacts.random()
                        
                        // Simulate a message from this contact
                        if (messageRepository is FakeMessageRepository) {
                            val randomMessage = messageTemplates.random()
                            messageRepository.simulateIncomingMessage(randomContact.id, randomMessage)
                            Log.d(TAG, "Simulated message from ${randomContact.name}: $randomMessage")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error simulating message", e)
                }

                // Wait for a random time between 30 seconds and 2 minutes before the next message
                val nextMessageDelay = Random.nextLong(
                    TimeUnit.SECONDS.toMillis(30),
                    TimeUnit.MINUTES.toMillis(2)
                )
                delay(nextMessageDelay)
            }
        }
    }

    /**
     * Cancels any ongoing message simulation and resets the simulation job.
     */
    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        Log.d(TAG, "Stopped message simulation")
    }

    /**
     * Retrieves the list of contacts from the repository if it is a `FakeContactRepository`.
     *
     * Uses reflection to access the private contacts field in the fake repository. Returns an empty list if the repository is not a `FakeContactRepository` or if an error occurs.
     *
     * @return A list of contacts available for simulation, or an empty list if unavailable.
     */
    private suspend fun getContacts(): List<tech.ziasvannes.safechat.data.models.Contact> {
        return try {
            // Attempt to get contacts directly if using FakeContactRepository
            if (contactRepository is FakeContactRepository) {
                val contactsField = contactRepository.javaClass.getDeclaredField("contacts")
                contactsField.isAccessible = true
                val contacts = contactsField.get(contactRepository) as? List<*>
                contacts?.filterIsInstance<tech.ziasvannes.safechat.data.models.Contact>() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving contacts for simulation", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "TestMessageSimulator"
    }
}