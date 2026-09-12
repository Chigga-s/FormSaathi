package com.formsaathi.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Manages Storage Access Framework (SAF) document creation, local file caching,
 * FileProvider content URIs, and Android View/Share intents without requiring broad storage permissions.
 */
class OutputFileManager(private val context: Context) {

    /**
     * Prepares an Intent for Android's Storage Access Framework document creator.
     * Suggests "Completed_Form.pdf" as the default output title.
     */
    fun createSaveFileIntent(suggestedFilename: String = "Completed_Form.pdf"): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, suggestedFilename)
        }
    }

    /**
     * Creates an internal temporary output PDF file inside the app cache directory.
     */
    fun createCachePdfFile(filename: String = "Completed_Form.pdf"): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val target = File(exportDir, filename)
        if (target.exists()) {
            target.delete()
        }
        return target
    }

    /**
     * Converts an internal File to a secure, shareable content URI using Android FileProvider.
     */
    fun getShareableUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Builds an Intent to view/open the completed PDF in any installed PDF viewer.
     */
    fun createOpenPdfIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Builds an Intent to share the completed PDF via the Android system share sheet.
     */
    fun createSharePdfIntent(uri: Uri, subject: String = "FormSaathi Completed Form"): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share Completed Form").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Copies data safely between streams with buffer flushing.
     */
    fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
        output.flush()
    }
}
