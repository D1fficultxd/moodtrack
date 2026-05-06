package com.d1ff.moodtrack.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.SettingsRepository
import com.d1ff.moodtrack.reminder.ReminderManager
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MoodViewModel = viewModel()) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val reminderEnabled by settingsRepo.reminderEnabled.collectAsState(initial = false)
    val reminderHour by settingsRepo.reminderHour.collectAsState(initial = 20)
    val reminderMinute by settingsRepo.reminderMinute.collectAsState(initial = 0)
    val language by settingsRepo.language.collectAsState(initial = "SYSTEM")
    
    var showTimePicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                settingsRepo.setReminderEnabled(true)
                ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SectionCard(stringResource(R.string.notifications), Icons.Default.Notifications) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.daily_reminder), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    coroutineScope.launch {
                                        settingsRepo.setReminderEnabled(true)
                                        ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                coroutineScope.launch {
                                    settingsRepo.setReminderEnabled(true)
                                    ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                settingsRepo.setReminderEnabled(false)
                                ReminderManager.cancelReminder(context)
                            }
                        }
                    }
                )
            }
            
            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedCard(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTimePicker = true 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        SectionCard(stringResource(R.string.language), Icons.Default.Language) {
            Box {
                OutlinedCard(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showLanguageMenu = true 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val langText = when(language) {
                                "RU" -> stringResource(R.string.lang_ru)
                                "EN" -> stringResource(R.string.lang_en)
                                else -> stringResource(R.string.lang_system)
                            }
                            Text(langText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lang_system)) },
                        onClick = {
                            coroutineScope.launch {
                                settingsRepo.setLanguage("SYSTEM")
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                            }
                            showLanguageMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lang_ru)) },
                        onClick = {
                            coroutineScope.launch {
                                settingsRepo.setLanguage("RU")
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                            }
                            showLanguageMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lang_en)) },
                        onClick = {
                            coroutineScope.launch {
                                settingsRepo.setLanguage("EN")
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            }
                            showLanguageMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showClearConfirm = true 
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.delete_all_data))
        }

        Text(
            text = stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        settingsRepo.setReminderTime(timePickerState.hour, timePickerState.minute)
                        if (reminderEnabled) {
                            ReminderManager.scheduleReminder(context, timePickerState.hour, timePickerState.minute)
                        }
                    }
                    showTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deleteAll()
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.yes_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
