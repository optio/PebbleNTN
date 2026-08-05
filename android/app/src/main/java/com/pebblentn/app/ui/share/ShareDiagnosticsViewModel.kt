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
    /**
     * Which dataset the user chose to share: redacted or full (with street names). Defaults to
     * [ExportMode.FULL] — the full notification text is far more useful for adding missing
     * direction/turn-word translations, and the user reviews the exact payload below before sending.
     */
    val mode: ExportMode = ExportMode.FULL,
    /** JSON (redacted or raw per [mode]), capped for on-screen review. */
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
 * Prepares the diagnostics the user can email to help add app support. The user chooses between the
 * redacted (privacy-safe) dataset and the full one that keeps street names — the latter is more
 * valuable for adding missing direction/turn-word translations. It builds the exact payload that
 * will be attached (capped to [DiagnosticExporter.EMAIL_MAX_BYTES]) so the on-screen review matches
 * what is shared, and hands the full JSON to the caller for the email intent. Nothing is transmitted
 * here.
 */
class ShareDiagnosticsViewModel(
    private val exporter: DiagnosticExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareDiagnosticsState())
    val state: StateFlow<ShareDiagnosticsState> = _state.asStateFlow()

    /** The full capped payload to attach; populated once [prepare] completes. */
    private var capped: CappedExport? = null

    init {
        prepare(ExportMode.FULL)
    }

    /** Switch which dataset is shown/shared and rebuild the preview. */
    fun setMode(mode: ExportMode) {
        if (mode != _state.value.mode) prepare(mode)
    }

    private fun prepare(mode: ExportMode) {
        // Preserve the previous preview while rebuilding so the mode selector doesn't flash away.
        _state.value = _state.value.copy(loading = true, mode = mode)
        viewModelScope.launch {
            val export = exporter.buildCapped(mode, DiagnosticExporter.EMAIL_MAX_BYTES)
            capped = export
            val trimmedPreview = export.json.length > PREVIEW_CHARS
            _state.value = ShareDiagnosticsState(
                loading = false,
                mode = mode,
                previewText = if (trimmedPreview) export.json.take(PREVIEW_CHARS) else export.json,
                previewTrimmed = trimmedPreview,
                includedEvents = export.includedEvents,
                totalEvents = export.totalEvents,
                sizeBytes = export.sizeBytes,
                truncatedToFit = export.truncated,
            )
        }
    }

    /** The full payload to attach to the email, or null if not ready. */
    fun payloadJson(): String? = capped?.json

    /** The mode the payload was built for, so the caller can name/label the attachment. */
    fun currentMode(): ExportMode = _state.value.mode

    private companion object {
        /** How much of the JSON to render for review; the full file can be up to 10 MB. */
        const val PREVIEW_CHARS = 8_000
    }
}
