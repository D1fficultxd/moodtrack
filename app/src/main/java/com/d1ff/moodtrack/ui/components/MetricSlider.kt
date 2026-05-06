package com.d1ff.moodtrack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlin.math.roundToInt

@Composable
fun MetricSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isInteger: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isInteger) value.toInt().toString() else value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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
            steps = steps
        )
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
