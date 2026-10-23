package com.example.clinicai.ui.staff

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.clinicai.R
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.databinding.ActivityStaffBinding
import com.example.clinicai.ui.common.StateViewController
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.confirmDestructive
import com.example.clinicai.ui.common.container
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.toMessage
import com.example.clinicai.ui.main.MainActivity
import kotlinx.coroutines.launch

/** Clinic staff area: the website's /dashboard, as a drawer-navigated set of native screens. */
class StaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffBinding
    val session by appViewModels { StaffSessionViewModel(it.users, it.auth) }
    private lateinit var stateView: StateViewController
    private var graphReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        stateView = StateViewController(binding.gateState, this) { session.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                session.profile.collect { state ->
                    binding.gateState.root.show(state !is UiState.Success)
                    when (state) {
                        UiState.Loading -> stateView.showLoading()
                        is UiState.Empty -> stateView.hide()
                        is UiState.Error -> {
                            if (state.error is AppError.Forbidden) {
                                stateView.showError(getString(R.string.staff_not_staff))
                            } else {
                                stateView.showError(state.error.toMessage(this@StaffActivity))
                            }
                        }
                        is UiState.Success -> {
                            stateView.hide()
                            prepareNavigation()
                        }
                    }
                }
            }
        }
    }

    /** The nav graph is attached only after the backend confirmed this account is clinic staff. */
    private fun prepareNavigation() {
        if (graphReady) return
        graphReady = true

        val navHost = supportFragmentManager.findFragmentById(R.id.staffNavHost) as NavHostFragment
        val navController = navHost.navController
        navController.setGraph(R.navigation.nav_staff)

        val topLevel = setOf(
            R.id.staffOverviewFragment,
            R.id.staffDoctorsFragment,
            R.id.staffQueueFragment,
            R.id.staffAnalyticsFragment,
            R.id.staffMembersFragment,
            R.id.clinicSettingsFragment,
        )
        val appBar = AppBarConfiguration(topLevel, binding.drawerLayout)
        binding.toolbar.setupWithNavController(navController, appBar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu)
        binding.toolbar.setNavigationContentDescription(R.string.staff_nav_open)
        binding.navigationView.setupWithNavController(navController)

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_patient_app -> {
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                    true
                }
                R.id.action_staff_sign_out -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    confirmDestructive(
                        title = getString(R.string.action_sign_out),
                        message = getString(R.string.profile_sign_out_confirm),
                        confirmLabel = getString(R.string.action_sign_out),
                        cancelLabel = getString(R.string.action_cancel),
                    ) { signOut() }
                    true
                }
                else -> {
                    val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    handled
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> binding.drawerLayout.closeDrawer(GravityCompat.START)
                    !navController.popBackStack() -> finish()
                }
            }
        })
    }

    private fun signOut() {
        lifecycleScope.launch {
            container.auth.signOut()
            container.prefs.clearUserState()
            startActivity(Intent(this@StaffActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }
}
