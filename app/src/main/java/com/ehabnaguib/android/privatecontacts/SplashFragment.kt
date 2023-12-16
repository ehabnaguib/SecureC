package com.ehabnaguib.android.privatecontacts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import java.util.concurrent.Executor


class SplashFragment : Fragment() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val biometricManager = BiometricManager.from(requireActivity())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                executor = ContextCompat.getMainExecutor(requireActivity())
                biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            // Handle error - close the app or inform the user
                            requireActivity().finish() // Close the app
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            findNavController().navigate(SplashFragmentDirections.openContactList())
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // Handle the authentication failed case
                        }
                    })

                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication required")
                    .setSubtitle("Log in using your biometric credential")
                    // Use setAllowedAuthenticators to specify whether to allow biometric and/or device credential (PIN, pattern, or password)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                // Show biometric prompt on app start
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(requireActivity(), "fuck you", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(requireActivity(), "fuck you", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle("Setup Lock Screen")
                builder.setMessage("For your security, you need to set up a lock screen. Would you like to do this now?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finishAffinity()
                }
                builder.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                    // Handle the "No" case by closing the activity or whatever is appropriate for your app
                    requireActivity().finish()
                }
                builder.setCancelable(false)
                builder.show()

            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                Toast.makeText(requireActivity(), "fuck you", Toast.LENGTH_SHORT).show()
        }

    }

}