package com.pebblentn.app.ui.watchsettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsModel

/**
 * The values of one watch setting (REQ-ANDROID-021, #102 FR5/FR6), the current one selected.
 * Picking a value changes it at once, as on the watch. For a visual setting the preview of the
 * selected value sits above the list, from the reported watch's image set, in greyscale on a
 * black-and-white watch; the colour list also shows a swatch per row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionListScreen(
    setting: WatchSetting,
    model: WatchSettingsModel,
    queued: Boolean,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(WatchSettingsUi.title(setting))
    val selected = model[setting]
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            WatchSettingsUi.preview(setting, selected, model.platform)?.let { preview ->
                item(key = "preview") {
                    PreviewImage(preview, stringResource(R.string.cd_watch_preview, valueLabel(setting, selected, model)))
                }
            }
            if (queued) {
                item(key = "queued") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(
                            text = stringResource(R.string.watch_settings_queued),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            items(WatchSettingsUi.options(setting, model), key = { "value-$it" }) { value ->
                OptionRow(setting, value, model, selected = value == selected, onSelect = { onSelect(value) })
                HorizontalDivider()
            }
        }
    }
}

/** The watch screen with the selected value, centred at its own pixel size scaled up evenly. */
@Composable
private fun PreviewImage(preview: WatchPreview, description: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        // Nearest-neighbour scaling keeps the watch's pixels sharp instead of blurring them.
        Image(
            bitmap = ImageBitmap.imageResource(preview.drawable),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            colorFilter = if (preview.greyscale) GREYSCALE else null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.height(PREVIEW_HEIGHT),
        )
    }
}

@Composable
private fun OptionRow(
    setting: WatchSetting,
    value: Int,
    model: WatchSettingsModel,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val label = valueLabel(setting, value, model)
    ListItem(
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        leadingContent = if (setting == WatchSetting.ACCENT) {
            { Swatch(Color(WatchSettingsUi.ACCENT_COLOURS[value])) }
        } else {
            null
        },
        headlineContent = { Text(label) },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
    )
}

/** A round colour sample; decorative, since the row's text names the colour. */
@Composable
private fun Swatch(colour: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(colour, CircleShape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape),
    )
}

private val PREVIEW_HEIGHT = 200.dp

/** Saturation 0: what a black-and-white watch shows of the basalt images. */
private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
