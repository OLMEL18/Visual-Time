package com.kisss.visualtime.completion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionDeliveryCoordinatorTest {
    @Test
    fun staleTokenIsRejected() {
        val store = FakeCompletionTokenStore(activeToken = "new")
        val coordinator = CompletionDeliveryCoordinator(store)

        assertFalse(coordinator.claimForForeground("old"))
        assertTrue(store.isActive("new"))
    }

    @Test
    fun visibleAppKeepsTokenForForegroundDelivery() {
        val store = FakeCompletionTokenStore(activeToken = "session")
        val coordinator = CompletionDeliveryCoordinator(store)

        assertFalse(
            coordinator.claimForBackground(
                token = "session",
                appVisible = true,
                notificationsAvailable = true,
            ),
        )
        assertTrue(coordinator.claimForForeground("session"))
    }

    @Test
    fun permissionDenialPreservesClaimUntilForegroundReturn() {
        val store = FakeCompletionTokenStore(activeToken = "session")
        val coordinator = CompletionDeliveryCoordinator(store)

        assertFalse(
            coordinator.claimForBackground(
                token = "session",
                appVisible = false,
                notificationsAvailable = false,
            ),
        )
        assertTrue(coordinator.claimForForeground("session"))
    }

    @Test
    fun backgroundDeliveryConsumesTokenExactlyOnce() {
        val store = FakeCompletionTokenStore(activeToken = "session")
        val coordinator = CompletionDeliveryCoordinator(store)

        assertTrue(
            coordinator.claimForBackground(
                token = "session",
                appVisible = false,
                notificationsAvailable = true,
            ),
        )
        assertFalse(
            coordinator.claimForBackground(
                token = "session",
                appVisible = false,
                notificationsAvailable = true,
            ),
        )
        assertFalse(coordinator.claimForForeground("session"))
    }

    private class FakeCompletionTokenStore(
        private var activeToken: String? = null,
    ) : CompletionTokenStore {
        override fun activate(token: String) {
            activeToken = token
        }

        override fun clear(token: String?) {
            if (token == null || activeToken == token) activeToken = null
        }

        override fun isActive(token: String): Boolean = activeToken == token

        override fun consumeIfActive(token: String): Boolean {
            if (activeToken != token) return false
            activeToken = null
            return true
        }
    }
}
