package com.pebblentn.app.export

import android.content.Context
import android.content.Intent
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

    private fun fileName(mode: ExportMode): String = when (mode) {
        ExportMode.RULES_ONLY -> "pebblentn-rules.json"
        ExportMode.PRIVACY_SAFE -> "pebblentn-diagnostics-safe.json"
        ExportMode.FULL -> "pebblentn-diagnostics-full.json"
    }

    private companion object {
        const val EXPORT_DIR = "exports"
    }
}
