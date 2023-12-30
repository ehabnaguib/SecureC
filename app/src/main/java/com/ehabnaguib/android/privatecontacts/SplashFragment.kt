package com.ehabnaguib.android.privatecontacts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import java.util.concurrent.Executor


class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val executor: Executor
        val biometricPrompt: BiometricPrompt
        val promptInfo: BiometricPrompt.PromptInfo

        val biometricManager = BiometricManager.from(requireContext())

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                executor = ContextCompat.getMainExecutor(requireContext())

                biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            requireActivity().finishAndRemoveTask()
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            findNavController().navigate(SplashFragmentDirections.openContactList())
                        }
                    })

                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication required")
                    .setSubtitle("Log in using your biometric credential")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                            or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(requireContext(),
                    "There is no suitable hardware available to be used for authentication.", Toast.LENGTH_SHORT).show()

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(requireContext(),
                    "Authentication hardware not available. Try again later.", Toast.LENGTH_SHORT).show()

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Setup Lock Screen")
                builder.setMessage("For your security, you need to set up a lock screen. Would you like to do this now?")
                builder.setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finishAffinity()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    requireActivity().finishAndRemoveTask()
                }
                builder.setCancelable(false)
                builder.show()
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                Toast.makeText(requireContext(),
                    "There's a problem with the security of your hardware. You might need a security update.", Toast.LENGTH_SHORT).show()

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Toast.makeText(requireContext(),
                    "This type of authentication is not supported on your phone.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}