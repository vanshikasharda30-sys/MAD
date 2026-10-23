package com.example.clinicai.ui.auth

import android.app.Activity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.databinding.ActivityAuthBinding
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.container
import kotlinx.coroutines.launch

/**
 * Sign in / sign up / password recovery with the same Clerk instance the website uses. Started for
 * a result: callers get RESULT_OK once a session is active, RESULT_CANCELED if the user backs out.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    val viewModel by appViewModels { AuthViewModel(it.auth, it.users, it.passwordPolicy) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val nav = (supportFragmentManager.findFragmentById(binding.authNavHost.id) as NavHostFragment).navController
                if (!nav.popBackStack()) {
                    viewModel.cancelFlow()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                } else if (nav.currentDestination?.id == com.example.clinicai.R.id.signInFragment) {
                    viewModel.cancelFlow()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.events.collect { event ->
                        if (event is AuthEvent.Completed) finishSignedIn()
                    }
                }
                // Already signed in (e.g. a restored session): never ask again.
                launch {
                    if (container.auth.awaitReady() is AuthState.SignedIn) finishSignedIn()
                }
            }
        }
    }

    private fun finishSignedIn() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
