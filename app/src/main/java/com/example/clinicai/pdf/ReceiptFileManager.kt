package com.example.clinicai.pdf

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.clinicai.domain.ReceiptDocument
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Creates receipt PDFs in app-private cache and hands them to other apps through a FileProvider. */
class ReceiptFileManager(private val context: Context, private val generator: ReceiptPdfGenerator = ReceiptPdfGenerator()) {

    private val authority = "${context.packageName}.fileprovider"
    private val dir get() = File(context.cacheDir, "receipts").apply { mkdirs() }

    /** Generates (or regenerates) the PDF for [document] and returns the file. */
    suspend fun createPdf(document: ReceiptDocument): File = withContext(Dispatchers.IO) {
        val file = File(dir, document.fileName)
        file.outputStream().use { generator.write(document, it) }
        file
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, authority, file)

    fun shareIntent(file: File, chooserTitle: String): Intent {
        val uri = uriFor(file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ClinicAI booking confirmation")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, chooserTitle).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    fun viewIntent(file: File): Intent {
        val uri = uriFor(file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Copies the generated PDF to a location the user picked with the system file picker. */
    suspend fun copyTo(file: File, destination: Uri) = withContext(Dispatchers.IO) {
        // "wt" truncates: saving over an existing, longer file must not leave trailing bytes that corrupt the PDF.
        val out = context.contentResolver.openOutputStream(destination, "wt") ?: throw IOException("Cannot open $destination")
        out.use { sink -> file.inputStream().use { it.copyTo(sink) } }
    }
}
