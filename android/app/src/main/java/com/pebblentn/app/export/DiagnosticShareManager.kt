package com.pebblentn.app.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes an export payload to a temporary file and opens the Android Sharesheet for it
 * (REQ-DEBUG-006, REQ-SEC-008). Files live under cache/exports and are pruned on each new export;
 * they are exposed only through a temporary content URI. Nothing is sent automatically.
 */
class DiagnosticShareManager(private val context: Context) {

    fun share(json: String, mode: ExportMode) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply {
            deleteRecursively() // cleanup previous exports
            mkdirs()
        }
        val file = File(dir, fileName(mode))
        file.writeText(json)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PebbleNTN diagnostics (${mode.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Open an email app to send [json] as an attachment, prefilling the recipient and subject. Used
     * by the share-to-help flow. A `mailto:` selector constrains the target to email apps while the
     * ACTION_SEND extras still carry the attachment; if no email app is present we fall back to the
     * generic Sharesheet so the user is never stuck. Nothing is sent automatically — the user still
     * reviews and presses send in their mail app.
     */
    fun shareViaEmail(json: String, recipient: String, subject: String, body: String) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        val file = File(dir, fileName(ExportMode.PRIVACY_SAFE))
        file.writeText(json)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (recipient.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Restrict resolution to email apps without dropping the attachment extras.
            selector = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:") }
        }
        try {
            context.startActivity(send)
        } catch (_: ActivityNotFoundException) {
            // No email client: fall back to the general chooser (still carries recipient/subject).
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                if (recipient.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(fallback, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun fileName(mode: ExportMode): String = when (mode) {
        ExportMode.RULES_ONLY -> "pebblentn-rules.json"
        ExportMode.PRIVACY_SAFE -> "pebblentn-diagnostics-safe.json"
        ExportMode.FULL -> "pebblentn-diagnostics-full.json"
    }

    private companion object {
        const val EXPORT_DIR = "exports"
    }
}
