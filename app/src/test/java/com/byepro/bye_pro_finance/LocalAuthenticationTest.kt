package com.byepro.bye_pro_finance

import com.byepro.bye_pro_finance.data.LocalAuthentication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAuthenticationTest {

    @Test
    fun firstLaunchCreatesRequestedDefaultCredentialsAndHashOnly() {
        val settings = mutableMapOf<String, String>()
        val auth = manager(settings)

        auth.initializeIfNeeded()

        assertTrue(auth.isEnabled())
        assertEquals("admin", auth.currentUsername())
        assertTrue(auth.authenticate("admin", "admin"))
        assertFalse(auth.authenticate("admin", "wrong"))
        assertEquals(null, settings[LocalAuthentication.KEY_PASSWORD])
        assertNotEquals("admin", settings[LocalAuthentication.KEY_HASH])
        assertTrue(settings[LocalAuthentication.KEY_SALT].orEmpty().isNotBlank())
    }

    @Test
    fun credentialsCanBeChangedOnlyWithCurrentPassword() {
        val settings = mutableMapOf<String, String>()
        val auth = manager(settings)
        auth.initializeIfNeeded()

        val rejected = auth.changeCredentials(
            currentPassword = "incorrect",
            newUsername = "owner",
            newPassword = "new-secret"
        )
        assertEquals(
            LocalAuthentication.CredentialChangeResult.CURRENT_PASSWORD_INVALID,
            rejected
        )
        assertTrue(auth.authenticate("admin", "admin"))

        val changed = auth.changeCredentials(
            currentPassword = "admin",
            newUsername = "owner",
            newPassword = "new-secret"
        )
        assertEquals(LocalAuthentication.CredentialChangeResult.SUCCESS, changed)
        assertTrue(auth.authenticate("owner", "new-secret"))
        assertFalse(auth.authenticate("admin", "admin"))
        assertFalse(auth.authenticate("owner", "admin"))
    }

    @Test
    fun credentialsValidationUsesDocumentedMinimumLengths() {
        val settings = mutableMapOf<String, String>()
        val auth = manager(settings)
        auth.initializeIfNeeded()

        assertEquals(
            LocalAuthentication.CredentialChangeResult.USERNAME_TOO_SHORT,
            auth.changeCredentials("admin", "ab", "123456")
        )
        assertEquals(
            LocalAuthentication.CredentialChangeResult.PASSWORD_TOO_SHORT,
            auth.changeCredentials("admin", "owner", "12345")
        )
        assertTrue(auth.authenticate("admin", "admin"))
    }

    @Test
    fun loginCanBeDisabledAndReenabledWithoutLosingCredentials() {
        val settings = mutableMapOf<String, String>()
        val auth = manager(settings)
        auth.initializeIfNeeded()
        auth.changeCredentials("admin", "owner", "new-secret")

        auth.setEnabled(false)
        assertFalse(auth.isEnabled())
        assertTrue(auth.authenticate("", ""))

        auth.setEnabled(true)
        assertTrue(auth.isEnabled())
        assertTrue(auth.authenticate("owner", "new-secret"))
    }

    @Test
    fun initializationDoesNotResetExistingCredentialsOrDisabledState() {
        val settings = mutableMapOf<String, String>()
        val auth = manager(settings)
        auth.initializeIfNeeded()
        auth.changeCredentials("admin", "owner", "new-secret")
        auth.setEnabled(false)

        manager(settings).initializeIfNeeded()

        assertFalse(auth.isEnabled())
        assertTrue(auth.authenticate("owner", "new-secret"))
        assertFalse(auth.authenticate("admin", "admin"))
    }

    private fun manager(settings: MutableMap<String, String>) =
        LocalAuthentication(
            readSetting = { key -> settings[key] },
            writeSetting = { key, value -> settings[key] = value }
        )
}
