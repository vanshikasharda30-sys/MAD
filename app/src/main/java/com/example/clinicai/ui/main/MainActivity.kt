package com.example.clinicai.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.clinicai.R
import com.example.clinicai.data.remote.PaymentResult
import com.example.clinicai.databinding.ActivityMainBinding
import com.example.clinicai.ui.common.container
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

/** Hosts the patient-facing screens. Bottom navigation is shown only on top-level destinations. */
class MainActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id in TOP_LEVEL_DESTINATIONS
        }
    }

    // Razorpay reports checkout outcomes to the hosting Activity; hand them to whichever screen is waiting.
    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        val orderId = data?.orderId
        val paymentId = razorpayPaymentId ?: data?.paymentId
        val signature = data?.signature
        container.paymentResults.publish(
            if (orderId != null && paymentId != null && signature != null) {
                PaymentResult.Success(orderId, paymentId, signature)
            } else {
                PaymentResult.Failure(code = -1, description = null)
            },
        )
    }

    override fun onPaymentError(code: Int, description: String?, data: PaymentData?) {
        container.paymentResults.publish(PaymentResult.Failure(code, description))
    }

    private companion object {
        val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.findDoctorFragment,
            R.id.chatFragment,
            R.id.myAppointmentsFragment,
            R.id.profileFragment,
        )
    }
}
