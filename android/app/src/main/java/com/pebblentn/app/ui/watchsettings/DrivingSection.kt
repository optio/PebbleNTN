package com.pebblentn.app.ui.watchsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pebblentn.app.R
import com.pebblentn.app.core.DrivingSpeeds
import com.pebblentn.app.core.WatchSetting
import com.pebblentn.app.core.WatchSettingsModel
import kotlin.math.roundToInt

/** Test tags of the When driving controls (#132). */
object DrivingSectionTags {
    const val SECTION = "watch-settings-driving-section"
    const val SWITCH = "watch-settings-driving-switch"
    const val SLIDER = "watch-settings-driving-slider"
    const val RESET = "watch-settings-driving-reset"
}

/**
 * The When driving section of the Watch settings screen (#132 FR3): the Hide switch, the driving
 * speed of the unit in use with a slider in steps of 5 (greyed out while the switch is off), the
 * reset button when the speed is not the default, and the explanation. No watch preview (FR3.5).
 * [onChange] edits a setting like any other watch setting: it is synced to the watch (FR1).
 */
@Composable
internal fun DrivingSection(model: WatchSettingsModel, onChange: (WatchSetting, Int) -> Unit) {
    val hide = model.drivingHide
    val speedSetting = model.drivingSpeedSetting
    val labels = stringArrayResource(WatchSettingsUi.valueLabels(speedSetting, imperial = DrivingSpeeds.isImperial(model.units)))
    val current = model[speedSetting]
    val speedLabel = labels.getOrElse(current) { WatchSettingsUi.drivingSpeedValue(model).toString() }
    val defaultLabel = labels.getOrElse(speedSetting.defaultValue) { WatchSettingsUi.drivingDefaultValue(model).toString() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag(DrivingSectionTags.SECTION)) {
        Text(
            text = stringResource(R.string.watch_settings_section_driving),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    modifier = Modifier
                        .testTag(DrivingSectionTags.SWITCH)
                        .toggleable(
                            value = hide,
                            role = Role.Switch,
                            onValueChange = { on ->
                                onChange(WatchSetting.DRIVING_MODE, if (on) DrivingSpeeds.MODE_HIDE else DrivingSpeeds.MODE_SHOW)
                            },
                        ),
                    headlineContent = { Text(stringResource(R.string.watch_setting_driving_mode)) },
                    trailingContent = { Switch(checked = hide, onCheckedChange = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
                HorizontalDivider()
                SpeedRow(
                    hide = hide,
                    current = current,
                    count = speedSetting.valueCount,
                    speedLabel = speedLabel,
                    onSelect = { index -> if (index != current) onChange(speedSetting, index) },
                )
                if (WatchSettingsUi.showsDrivingReset(model)) {
                    TextButton(
                        onClick = { onChange(speedSetting, speedSetting.defaultValue) },
                        enabled = hide,
                        modifier = Modifier.padding(horizontal = 8.dp).testTag(DrivingSectionTags.RESET),
                    ) {
                        Text(stringResource(R.string.watch_driving_reset, defaultLabel))
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.watch_driving_explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/**
 * "Driving speed" with its value and a slider over the unit's list. The value is sent when the drag
 * ends, not on every step, so one change is one synced edit.
 */
@Composable
private fun SpeedRow(hide: Boolean, current: Int, count: Int, speedLabel: String, onSelect: (Int) -> Unit) {
    var position by remember(current, count) { mutableFloatStateOf(current.toFloat()) }
    val description = stringResource(R.string.cd_watch_driving_speed_slider, speedLabel)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (hide) 1f else DISABLED_ALPHA)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(stringResource(R.string.watch_setting_driving_speed), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.watch_driving_speed_value, speedLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = { onSelect(position.roundToInt().coerceIn(0, count - 1)) },
            valueRange = 0f..(count - 1).toFloat(),
            steps = (count - 2).coerceAtLeast(0),
            enabled = hide,
            modifier = Modifier
                .testTag(DrivingSectionTags.SLIDER)
                .semantics { contentDescription = description },
        )
    }
}

private const val DISABLED_ALPHA = 0.6f
