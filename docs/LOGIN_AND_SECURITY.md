# Welcome, Local Login and Security Specification

**Status:** Required Native application-entry contract  
**Owner:** `MainActivity` + `LocalAuthentication`  
**Persistence:** Existing SQLite `app_settings` table  
**Default locale:** Arabic (English is also translated)

## 1. Required launch flow

Every fresh application process starts with this order (returning from the Android app switcher does not by itself prove a new login session):

1. **Welcome screen** — shows the BYE PRO Finance introduction.
2. **Continue by swipe or button** — a horizontal swipe or the Continue action moves to the next stage.
3. **Login screen** — displayed when local login protection is enabled.
4. **Application** — displayed only after successful authentication.

When the user has disabled login protection, the welcome screen still appears, but Continue opens the application directly and skips the login screen.

Welcome and Login use the shared semantic palette and typography without generic centered cards. Welcome uses the BYE PRO signature, original finance-orbit artwork, strong statement typography and a progress rule. Login uses its own private-access hierarchy and open underlined fields. Arabic and English change text only; geometry remains RTL and unchanged.

## 2. Initial credentials

On the first Native installation, or the first startup after opening a compatible database that has no saved local-authentication salt/verifier, the application initializes:

| Field | Initial value |
|---|---|
| Username | `admin` |
| Password | `admin` |
| Login protection | Enabled |

These are bootstrap credentials, not a recommended permanent password. Change them immediately after the first successful login from:

**Settings → Login and security → Change login details**

Because the requested initial values are fixed, anyone who knows the application's initial configuration can try them. Do not leave the default password in place.

Initialization is idempotent: after a credential profile exists, normal launches must not reset the username, password hash, salt or enabled/disabled preference.

## 3. Changing login details

The settings flow asks for:

- Current password.
- New username.
- New password.
- Confirmation of the new password.

The update is accepted only when the current password is correct and both new password fields match.

| Validation | Requirement |
|---|---|
| Username | At least 3 characters after trimming outer spaces |
| Password | At least 6 characters |
| Confirmation | Must exactly match the new password |
| Current password | Required and verified before changing credentials |

After a successful update, the old username/password pair no longer authenticates.

## 4. Disabling and re-enabling login

Path: **Settings → Login and security**.

- **Disable login:** after confirmation, the welcome screen remains, but the next launch skips the login form.
- **Enable login:** after confirmation, the next launch requires the current saved username and password.
- Disabling login does not delete credentials. Re-enabling restores the existing credential profile.
- Only a user already inside the application can change this setting through its Settings screen.

## 5. Password storage

The Native app does not store the password as readable text.

For each password, `LocalAuthentication` derives a 256-bit verifier using:

- PBKDF2 with HMAC-SHA-256.
- 120,000 derivation iterations.
- A fresh cryptographically random 16-byte salt.
- Constant-time byte comparison when verifying a password.

The following keys are stored in the existing `app_settings` table:

| Key | Contents |
|---|---|
| `local_auth_username` | Current username |
| `local_auth_salt` | Base64-encoded random salt |
| `local_auth_password_hash` | Base64-encoded derived verifier |
| `local_auth_enabled` | `true` or `false` |

Changing a password generates a new salt and verifier. Existing finance tables, work records, household expenses, bills, rent, debts and payment histories are not modified by login changes.

## 6. Security boundary and limitations

This is a **device-local application gate**, not a cloud account system.

It does not provide:

- Server-side identity verification or account synchronization.
- Password reset by email, SMS or a remote administrator.
- Protection against an attacker who can control an already-unlocked device or access private app files with elevated/root privileges.
- Encryption of the finance database itself.
- A guarantee that a person using the same unlocked device cannot access the app after the login screen has been passed.

The existing SQLite database remains in Android's private application storage. Password hashing protects the stored verifier from being a direct readable password, but it does not encrypt the financial tables.

There is no password-recovery flow in this stage. Keep the updated credentials safely. Do not clear app data or uninstall the app as a way to reset the password; doing so can remove local finance data. Any future recovery feature must be designed separately and must not silently erase records.

## 7. Design System and Arabic/English behaviour

Welcome and Login use the shared semantic palette and typography, but not the generic centered-card layout. Welcome uses original finance-orbit artwork, a BYE PRO signature, statement-led typography and a progress rule. Login uses a separate private-access hierarchy and open underlined fields instead of a boxed stock form. The screen direction remains RTL in both languages; English changes resource text only.

The Welcome screen supports both the visible Continue button and a horizontal swipe gesture. The Login form pre-fills the current username, masks the password field and shows a localized error without opening the app when credentials are incorrect.

## 8. Implementation and tests

| Concern | Implementation |
|---|---|
| Launch screens and routing | `MainActivity.kt` |
| Credential initialization, verification and update | `data/LocalAuthentication.kt` |
| Settings persistence | `data/LegacyFinanceDatabase.kt` → `app_settings` |
| Bilingual screen copy | `res/values/strings.xml`, `res/values-en/strings.xml` |
| Credential behaviour tests | `app/src/test/.../LocalAuthenticationTest.kt` |

Required test cases:

- Initial credentials authenticate, incorrect password is rejected.
- Plain password is not persisted.
- Changing credentials requires the current password.
- Username and password minimum lengths are enforced.
- Disabled login skips authentication without deleting credentials.
- Re-enabling login restores authentication.
- Startup does not overwrite existing credential or enabled/disabled state.

A successful unit test or debug APK build verifies implementation behaviour in the configured test environment. Device acceptance must still check swipe gestures, keyboard behaviour, password masking, rotation/re-entry, Arabic/English text and both appearance modes.


## 9. Manual device acceptance checklist

- [ ] Fresh process opens Welcome first.
- [ ] Tapping Continue opens Login while login protection is enabled.
- [ ] Horizontal swipe on the Welcome surface also continues.
- [ ] `admin` / `admin` succeeds only while the initial profile is still unchanged.
- [ ] Incorrect username or password remains on Login and displays the localized error.
- [ ] Password is visually masked and the keyboard does not obscure the form on the target phone.
- [ ] Settings can change the username and password only after the current password is supplied.
- [ ] Password confirmation mismatch is rejected.
- [ ] Disabling login takes effect after a fresh process launch and does not delete the stored profile.
- [ ] Re-enabling login requires the current saved credentials after a fresh process launch.
- [ ] Repeat the checks in Arabic and English and in Light and Dark appearance modes.
- [ ] Confirm existing finance records remain untouched after changing login settings.
