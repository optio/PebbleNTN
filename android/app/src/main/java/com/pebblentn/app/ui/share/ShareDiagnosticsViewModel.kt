package com.pebblentn.app.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pebblentn.app.export.CappedExport
import com.pebblentn.app.export.DiagnosticExporter
import com.pebblentn.app.export.ExportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the share-to-help screen. */
data class ShareDiagnosticsState(
    val loading: Boolean = true,
    /** Redacted JSON, capped for on-screen review (not the whole attachment when it is large). */
    val previewText: String = "",
    /** Whether [previewText] is a shortened view of a larger attachment. */
    val previewTrimmed: Boolean = false,
    val includedEvents: Int = 0,
    val totalEvents: Int = 0,
    val sizeBytes: Int = 0,
    /** True when older events were dropped to fit the 10 MB attachment budget. */
    val truncatedToFit: Boolean = false,
) {
    val hasContent: Boolean get() = includedEvents > 0
}

/**
 * Prepares the privacy-safe diagnostics the user can email to help add app support. It builds the
 * exact payload that will be attached (redacted, capped to [DiagnosticExporter.EMAIL_MAX_BYTES]) so
 * the on-screen review matches what is shared, and hands the full JSON to the caller for the email
 * intent. Nothing is transmitted here.
 */
class ShareDiagnosticsViewModel(
    private val exporter: DiagnosticExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareDiagnosticsState())
    val state: StateFlow<ShareDiagnosticsState> = _state.asStateFlow()

    /** The full capped payload to attach; populated once [prepare] completes. */
    private var capped: CappedExport? = null

    init {
        prepare()
    }

    fun prepare() {
        _state.value = ShareDiagnosticsState(loading = true)
        viewModelScope.launch {
            val export = exporter.buildCapped(ExportMode.PRIVACY_SAFE, DiagnosticExporter.EMAIL_MAX_BYTES)
            capped = export
            val trimmedPreview = export.json.length > PREVIEW_CHARS
            _state.value = ShareDiagnosticsState(
                loading = false,
                previewText = if (trimmedPreview) export.json.take(PREVIEW_CHARS) else export.json,
                previewTrimmed = trimmedPreview,
                includedEvents = export.includedEvents,
                totalEvents = export.totalEvents,
                sizeBytes = export.sizeBytes,
                truncatedToFit = export.truncated,
            )
        }
    }

    /** The full redacted payload to attach to the email, or null if not ready. */
    fun payloadJson(): String? = capped?.json

    private companion object {
        /** How much of the redacted JSON to render for review; the full file can be up to 10 MB. */
        const val PREVIEW_CHARS = 8_000
    }
}
