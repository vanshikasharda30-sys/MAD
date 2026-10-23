package com.example.clinicai.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clinicai.domain.ReceiptDocument
import com.example.clinicai.domain.ReceiptFormatter
import com.example.clinicai.domain.ReceiptRow
import com.example.clinicai.domain.ReceiptSection
import com.example.clinicai.testing.TestData
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptPdfInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun pdfBytes(document: ReceiptDocument): ByteArray =
        ByteArrayOutputStream().also { ReceiptPdfGenerator().write(document, it) }.toByteArray()

    private fun pageCount(file: File): Int {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { return it.pageCount }
        }
    }

    private fun renderFirstPage(file: File): Bitmap {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    @Test fun generatesAValidPdfDocument() {
        val bytes = pdfBytes(ReceiptFormatter.format(TestData.receipt()))
        assertTrue("PDF header", String(bytes, 0, 5, Charsets.US_ASCII) == "%PDF-")
        assertTrue("non-trivial size: ${bytes.size}", bytes.size > 2_000)
    }

    @Test fun pdfRendersVisibleContentOnAnA4Page() {
        val file = File(context.cacheDir, "instrumented-receipt.pdf").also { it.writeBytes(pdfBytes(ReceiptFormatter.format(TestData.receipt()))) }
        assertEquals(1, pageCount(file))
        val bitmap = renderFirstPage(file)
        assertEquals(595, bitmap.width)
        assertEquals(842, bitmap.height)

        // The brand band is dark navy across the top and the body contains dark text on white.
        val top = bitmap.getPixel(bitmap.width / 2, 10)
        assertTrue("brand band should be dark, was #${Integer.toHexString(top)}", Color.red(top) < 60 && Color.blue(top) < 90)
        var darkPixels = 0
        for (y in 120 until 700 step 3) for (x in 40 until 560 step 3) if (Color.red(bitmap.getPixel(x, y)) < 100) darkPixels++
        assertTrue("page body should contain text, dark pixels=$darkPixels", darkPixels > 200)
    }

    @Test fun longReceiptsFlowOntoAdditionalPages() {
        val rows = (1..70).map { ReceiptRow("Field $it", "Value number $it with some descriptive text") }
        val base = ReceiptFormatter.format(TestData.receipt())
        val document = base.copy(sections = listOf(ReceiptSection("Long section", rows)))
        val file = File(context.cacheDir, "instrumented-long.pdf").also { it.writeBytes(pdfBytes(document)) }
        assertTrue("expected multiple pages, got ${pageCount(file)}", pageCount(file) >= 2)
    }

    @Test fun depositReceiptAlsoFitsOnOnePage() {
        val file = File(context.cacheDir, "instrumented-deposit.pdf")
            .also { it.writeBytes(pdfBytes(ReceiptFormatter.format(TestData.receipt(deposit = 10000, depositStatus = "PAID")))) }
        assertTrue(file.length() > 2_000)
        assertEquals(1, pageCount(file))
    }

    @Test fun fileManagerCreatesAShareableFileThroughFileProvider() = runBlocking {
        val manager = ReceiptFileManager(context)
        val document = ReceiptFormatter.format(TestData.receipt())
        val file = manager.createPdf(document)
        assertTrue(file.exists() && file.length() > 2_000)
        assertEquals(document.fileName, file.name)

        val intent = manager.shareIntent(file, "Share booking receipt")
        assertEquals(Intent.ACTION_CHOOSER, intent.action)
        val send = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("application/pdf", send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        val uri = send.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
        assertNotNull(uri)
        assertEquals("${context.packageName}.fileprovider", uri!!.authority)
        // Another app reading through the URI must see the same PDF bytes.
        val head = context.contentResolver.openInputStream(uri)!!.use { it.readNBytes(5) }
        assertEquals("%PDF-", String(head, Charsets.US_ASCII))

        val view = manager.viewIntent(file)
        assertEquals(Intent.ACTION_VIEW, view.action)
        assertEquals("application/pdf", view.type)
        Unit
    }

    @Test fun copyToWritesTheExportedBytes() = runBlocking {
        val manager = ReceiptFileManager(context)
        val file = manager.createPdf(ReceiptFormatter.format(TestData.receipt()))
        val target = File(context.cacheDir, "exported-copy.pdf")
        // Saving over an existing, longer file must not leave trailing bytes behind.
        target.writeBytes(ByteArray(file.length().toInt() + 5_000) { 'x'.code.toByte() })
        manager.copyTo(file, android.net.Uri.fromFile(target))
        assertEquals(file.length(), target.length())
        assertTrue(target.readBytes().contentEquals(file.readBytes()))
    }

    /** Leaves a sample PDF on external storage so the layout can be inspected by eye. */
    @Test fun writeSamplesForManualInspection() {
        val dir = context.getExternalFilesDir(null)!!
        listOf(
            "sample-confirmation" to TestData.receipt(),
            "sample-deposit" to TestData.receipt(10000, "PAID"),
        ).forEach { (name, receipt) ->
            val pdf = File(dir, "$name.pdf").also { it.writeBytes(pdfBytes(ReceiptFormatter.format(receipt))) }
            ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, android.graphics.Matrix().apply { setScale(2f, 2f) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            File(dir, "$name-p${i + 1}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
                        }
                    }
                }
            }
        }
    }
}
