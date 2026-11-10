package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import java.security.MessageDigest

class SecurityHelper(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("finangest_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_TWO_FACTOR_ENABLED = "two_factor_enabled"
        private const val KEY_DARK_MODE = "dark_mode_preference" // "SYSTEM", "LIGHT", "DARK"
    }

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRICS_ENABLED, value) }

    var isTwoFactorEnabled: Boolean
        get() = prefs.getBoolean(KEY_TWO_FACTOR_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_TWO_FACTOR_ENABLED, value) }

    var darkModePreference: String
        get() = prefs.getString(KEY_DARK_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit { putString(KEY_DARK_MODE, value) }

    fun hasPin(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit { putString(KEY_PIN_HASH, hash) }
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return true
        return savedHash == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun authenticateWithBiometrics(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = activity.mainExecutor
            val biometricPrompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle("FinanGest - Autenticação Segura")
                .setSubtitle("Confirme sua biometria para acessar suas finanças")
                .setDescription("Seus dados financeiros são protegidos por criptografia")
                .setNegativeButton("Usar PIN", executor) { _, _ ->
                    onError("PIN_REQUESTED")
                }
                .build()

            val cancellationSignal = android.os.CancellationSignal()
            biometricPrompt.authenticate(
                cancellationSignal,
                executor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString?.toString() ?: "Falha na biometria")
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Biometria não reconhecida")
                    }
                }
            )
        } else {
            // Older Android versions or fallback
            onSuccess()
        }
    }
}
