package com.ehabnaguib.android.securec

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import com.ehabnaguib.android.securec.databinding.ActivityMainBinding
import com.ehabnaguib.android.securec.utils.AuthenticationHandler


class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding : ActivityMainBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val mainActivityViewModel : MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and content from being shown in the recent apps list
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }



    override fun onResume() {
        super.onResume()

        // Requesting authentication everytime the user gets back to the app for security.
        // App doesn't work if there's no lock screen set up on the phone and notifies the user.
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
        val fragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (mainActivityViewModel.shouldAuthenticate) {
            binding.securityOverlay.visibility = VISIBLE
            AuthenticationHandler.startAuthentication(
                fragment = fragment!!,
                onAuthenticationSuccess = {
                    mainActivityViewModel.shouldAuthenticate = false
                    binding.securityOverlay.visibility = GONE
                },
                onNoneEnrolled = {
                    // Prompt the user to set up a lock screen and biometric credentials
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Setup Lock Screen")
                    builder.setMessage("For your security, you need to set up a lock screen. Would you like to do this now?")
                    builder.setPositiveButton("Yes") { _, _ ->
                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        this.finishAffinity()
                    }
                    builder.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        this.finishAndRemoveTask()
                    }
                    builder.setCancelable(false)
                    builder.show()
                }
            )
        }
        else
            binding.securityOverlay.visibility = GONE
    }


    override fun onRestart() {
        super.onRestart()
        mainActivityViewModel.shouldAuthenticate = true
    }
}


