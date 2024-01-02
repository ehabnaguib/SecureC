package com.ehabnaguib.android.securec.utils

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.concurrent.Executor

object AuthenticationHandler {

    fun startAuthentication(
        fragment: Fragment,
        onAuthenticationSuccess: () -> Unit,
        onNoneEnrolled: () -> Unit
    ) {
        val context = fragment.requireContext()
        val executor: Executor = ContextCompat.getMainExecutor(context)
        val biometricManager = BiometricManager.from(context)


        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val biometricPrompt = BiometricPrompt(fragment, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onAuthenticationSuccess()
                    }
                })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication required")
                    .setSubtitle("Log in using your biometric credential")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(context,
                    "There is no suitable hardware available to be used for authentication.", Toast.LENGTH_SHORT).show()
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(context,
                    "Authentication hardware not available. Try again later.", Toast.LENGTH_SHORT).show()
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onNoneEnrolled()
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                Toast.makeText(context,
                    "There's a problem with the security of your hardware. You might need a security update.", Toast.LENGTH_SHORT).show()

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Toast.makeText(context,
                    "This type of authentication is not supported on your phone.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}