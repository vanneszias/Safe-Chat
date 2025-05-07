package tech.ziasvannes.safechat

import android.app.Application
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import tech.ziasvannes.safechat.testing.FakeMessageRepository
import tech.ziasvannes.safechat.testing.TestMessageSimulator
import tech.ziasvannes.safechat.testing.TestMode
import tech.ziasvannes.safechat.testing.TestModePrefs

@HiltAndroidApp
class SafeChatApplication : Application() {

    /**
     * Initializes the application and configures test mode settings on startup.
     *
     * Sets up the application to use test repositories by default, disables simulation of incoming
     * messages and connection issues, and initializes test components if required.
     */
    override fun onCreate() {
        super.onCreate()

        TestModePrefs.load(this)

        // By default, enable test repositories for UI development
        // This can be toggled at runtime through the TestSettingsScreen
        TestMode.configure(
                useTestRepositories = true,
                simulateIncomingMessages = false,
                simulateConnectionIssues = false
        )

        if (TestMode.useTestRepositories) {
            val entryPoint =
                    EntryPointAccessors.fromApplication(this, TestRepoEntryPoint::class.java)
            val fakeRepo = entryPoint.fakeMessageRepository()
            if (fakeRepo is FakeMessageRepository) {
                TestMessageSimulator.start(fakeRepo)
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(SingletonComponent::class)
interface TestRepoEntryPoint {
    @Named("test")
    fun fakeMessageRepository(): tech.ziasvannes.safechat.domain.repository.MessageRepository
}
