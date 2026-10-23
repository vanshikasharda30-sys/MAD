package com.example.clinicai.ui.booking

import android.content.ActivityNotFoundException
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.clinicai.R
import com.example.clinicai.domain.ReceiptData
import com.example.clinicai.domain.ReceiptFormatter
import com.example.clinicai.pdf.ReceiptFileManager
import com.example.clinicai.ui.common.snack
import java.io.File
import kotlinx.coroutines.launch

/**
 * Save / share / open actions for a receipt PDF. Create it as a property of the fragment so the
 * file-picker launcher is registered before the fragment starts.
 */
class ReceiptActions(private val fragment: Fragment) {

    private val files by lazy { ReceiptFileManager(fragment.requireContext().applicationContext) }
    private var pendingSave: File? = null

    private val saveLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val file = pendingSave
        pendingSave = null
        if (uri == null || file == null) return@registerForActivityResult
        fragment.lifecycleScope.launch {
            try {
                files.copyTo(file, uri)
                message(R.string.receipt_saved)
            } catch (e: Exception) {
                message(R.string.receipt_save_failed)
            }
        }
    }

    fun save(receipt: ReceiptData) = withPdf(receipt) { document, file ->
        pendingSave = file
        saveLauncher.launch(document.fileName)
    }

    fun share(receipt: ReceiptData) = withPdf(receipt) { _, file ->
        fragment.startActivity(files.shareIntent(file, fragment.getString(R.string.receipt_share_chooser)))
    }

    fun open(receipt: ReceiptData) = withPdf(receipt) { _, file ->
        try {
            fragment.startActivity(files.viewIntent(file))
        } catch (e: ActivityNotFoundException) {
            message(R.string.receipt_no_pdf_viewer)
        }
    }

    private fun withPdf(receipt: ReceiptData, block: (com.example.clinicai.domain.ReceiptDocument, File) -> Unit) {
        fragment.lifecycleScope.launch {
            val document = ReceiptFormatter.format(receipt)
            val file = try {
                files.createPdf(document)
            } catch (e: Exception) {
                message(R.string.receipt_generation_failed)
                return@launch
            }
            block(document, file)
        }
    }

    private fun message(resId: Int) {
        val view: View = fragment.view ?: return
        view.snack(fragment.getString(resId))
    }
}
