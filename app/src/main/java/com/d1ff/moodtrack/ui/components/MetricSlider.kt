package com.d1ff.moodtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.d1ff.moodtrack.R
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MetricSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isInteger: Boolean = true,
    label: String? = null,
    onHelpClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { showDialog = true }
                )
                if (onHelpClick != null) {
                    IconButton(
                        onClick = onHelpClick,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Help",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Surface(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
            ) {
                Text(
                    text = if (isInteger) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        Slider(
            value = value,
            onValueChange = { newValue ->
                val roundedValue = if (isInteger) {
                    newValue.roundToInt().toFloat()
                } else {
                    (newValue * 2).roundToInt() / 2f
                }
                
                if (roundedValue != value) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(roundedValue)
                }
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.34f),
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f)
            )
        )

        if (label != null) {
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (showDialog) {
        var textValue by remember { mutableStateOf(if (isInteger) value.toInt().toString() else value.toString()) }
        var isError by remember { mutableStateOf(false) }
        val errorMessage = stringResource(R.string.error_invalid_range, range.start, range.endInclusive)

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.manual_entry_title)) },
            text = {
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { 
                            textValue = it
                            isError = false
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text(errorMessage)
                            } else {
                                Text(stringResource(R.string.error_invalid_range, range.start, range.endInclusive))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = textValue.replace(",", ".").toFloatOrNull()
                    if (parsed != null && parsed in range) {
                        onValueChange(parsed)
                        showDialog = false
                    } else {
                        isError = true
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
