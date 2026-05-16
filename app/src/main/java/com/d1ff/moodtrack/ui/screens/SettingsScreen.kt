package com.d1ff.moodtrack.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.SettingsRepository
import com.d1ff.moodtrack.health.HealthConnectAvailability
import com.d1ff.moodtrack.health.HealthConnectSleepManager
import com.d1ff.moodtrack.reminder.ReminderManager
import com.d1ff.moodtrack.ui.components.ExpressiveSectionHeader
import com.d1ff.moodtrack.ui.components.GlassCard
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import com.d1ff.moodtrack.viewmodel.PdfImportState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MoodViewModel = viewModel(),
    onNavigateToGuide: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val reminderEnabled by settingsRepo.reminderEnabled.collectAsState(initial = false)
    val reminderHour by settingsRepo.reminderHour.collectAsState(initial = 20)
    val reminderMinute by settingsRepo.reminderMinute.collectAsState(initial = 0)
    val language by settingsRepo.language.collectAsState(initial = "SYSTEM")
    val healthConnectAutoFill by settingsRepo.healthConnectAutoFill.collectAsState(initial = false)
    val healthConnectAskBeforeReplace by settingsRepo.healthConnectAskBeforeReplace.collectAsState(initial = true)
    val pdfImportState by viewModel.pdfImportState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val healthSleepManager = remember { HealthConnectSleepManager(context) }
    var healthAvailability by remember { mutableStateOf(healthSleepManager.availability()) }
    var hasSleepPermission by remember { mutableStateOf(false) }
    
    var showTimePicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val importCompletedText = stringResource(R.string.import_completed)
    val importAddedText = stringResource(R.string.import_result_added)
    val importUpdatedText = stringResource(R.string.import_result_updated)
    val importSkippedText = stringResource(R.string.import_result_skipped)
    val importFailedText = stringResource(R.string.import_file_failed)
    val importFormatNotRecognizedText = stringResource(R.string.import_format_not_recognized)
    val healthConnectedText = stringResource(R.string.health_connect_connected)
    val healthNoPermissionText = stringResource(R.string.health_connect_no_sleep_permission)
    val healthUnavailableText = stringResource(R.string.health_connect_unavailable)

    LaunchedEffect(healthAvailability) {
        if (healthAvailability == HealthConnectAvailability.Available) {
            hasSleepPermission = healthSleepManager.hasSleepPermission()
        } else {
            hasSleepPermission = false
        }
    }

    LaunchedEffect(pdfImportState) {
        when (val state = pdfImportState) {
            is PdfImportState.Completed -> {
                snackbarHostState.showSnackbar(
                    listOf(
                        importCompletedText,
                        "$importAddedText: ${state.result.addedEntries}",
                        "$importUpdatedText: ${state.result.updatedEntries}",
                        "$importSkippedText: ${state.result.skippedRows}"
                    ).joinToString("\n")
                )
                viewModel.clearPdfImportState()
            }
            PdfImportState.FileError -> {
                snackbarHostState.showSnackbar(importFailedText)
                viewModel.clearPdfImportState()
            }
            PdfImportState.FormatNotRecognized -> {
                snackbarHostState.showSnackbar(importFormatNotRecognizedText)
                viewModel.clearPdfImportState()
            }
            PdfImportState.Idle,
            PdfImportState.Loading -> Unit
        }
    }

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

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        hasSleepPermission = grantedPermissions.contains(healthSleepManager.sleepPermission)
        coroutineScope.launch {
            snackbarHostState.showSnackbar(if (hasSleepPermission) healthConnectedText else healthNoPermissionText)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.guide_title),
                    icon = Icons.AutoMirrored.Filled.HelpOutline
                ) {
                    SettingsInnerBox(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToGuide()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.guide_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.notifications),
                    icon = Icons.Default.Notifications
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.daily_reminder), 
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

                    AnimatedVisibility(visible = reminderEnabled) {
                        SettingsInnerBox(
                            onClick = {
                                showTimePicker = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.reminder_time), 
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.health_connect_title),
                    icon = Icons.Default.Favorite
                ) {
                    Text(
                        text = stringResource(R.string.health_connect_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    when (healthAvailability) {
                        HealthConnectAvailability.Available -> {
                            SettingsInnerBox(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    healthPermissionLauncher.launch(healthSleepManager.permissions)
                                }
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = stringResource(R.string.health_connect_connect),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = if (hasSleepPermission) {
                                                stringResource(R.string.health_connect_connected)
                                            } else {
                                                stringResource(R.string.health_connect_no_sleep_permission)
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Bedtime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SettingsSwitchRow(
                                title = stringResource(R.string.health_connect_auto_fill),
                                checked = healthConnectAutoFill,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch { settingsRepo.setHealthConnectAutoFill(enabled) }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            SettingsSwitchRow(
                                title = stringResource(R.string.health_connect_ask_before_replace),
                                checked = healthConnectAskBeforeReplace,
                                enabled = healthConnectAutoFill,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch { settingsRepo.setHealthConnectAskBeforeReplace(enabled) }
                                }
                            )
                        }

                        HealthConnectAvailability.InstallOrUpdateRequired -> {
                            SettingsInnerBox(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    runCatching { context.startActivity(healthSleepManager.installIntent()) }
                                        .onFailure {
                                            coroutineScope.launch { snackbarHostState.showSnackbar(healthUnavailableText) }
                                        }
                                }
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = stringResource(R.string.health_connect_install),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = stringResource(R.string.health_connect_unavailable),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }

                        HealthConnectAvailability.Unavailable -> {
                            Text(
                                text = stringResource(R.string.health_connect_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.health_connect_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.settings_data),
                    icon = Icons.Default.Storage
                ) {
                    SettingsInnerBox(
                        onClick = {
                            if (pdfImportState !is PdfImportState.Loading) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                        }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(R.string.import_data_title),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.import_data_subtitle),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.UploadFile,
                                    contentDescription = stringResource(R.string.import_data_content_description),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = {
                                if (pdfImportState is PdfImportState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showClearConfirm = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_all_data), fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.language),
                    icon = Icons.Default.Language
                ) {
                    SettingsInnerBox(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showLanguageSheet = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val langText = when(language) {
                                "RU" -> stringResource(R.string.lang_ru)
                                "EN" -> stringResource(R.string.lang_en)
                                else -> stringResource(R.string.lang_system)
                            }
                            Text(
                                text = langText,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(R.string.support_author),
                    icon = Icons.Default.Favorite
                ) {
                    SettingsInnerBox(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://boosty.to/d1fficultxd"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    stringResource(R.string.support_author), 
                                    style = MaterialTheme.typography.bodyLarge, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.support_author_subtitle), 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp)
                )
            }
        }
    }

    if (showTimePicker) {
        val configuration = LocalConfiguration.current
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        var lastHapticHour by remember { mutableIntStateOf(timePickerState.hour) }
        var lastHapticMinute by remember { mutableIntStateOf(timePickerState.minute) }

        LaunchedEffect(Unit) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        LaunchedEffect(timePickerState.hour) {
            if (lastHapticHour != timePickerState.hour) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHapticHour = timePickerState.hour
            }
        }

        LaunchedEffect(timePickerState.minute) {
            if (lastHapticMinute != timePickerState.minute) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHapticMinute = timePickerState.minute
            }
        }
        
        ModalBottomSheet(
            onDismissRequest = { showTimePicker = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = configuration.screenHeightDp.dp * 0.86f)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.reminder_time),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                settingsRepo.setReminderTime(timePickerState.hour, timePickerState.minute)
                                if (reminderEnabled) {
                                    ReminderManager.scheduleReminder(context, timePickerState.hour, timePickerState.minute)
                                }
                            }
                            showTimePicker = false
                        },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LanguageOptionRow(
                    text = stringResource(R.string.lang_system),
                    selected = language == "SYSTEM",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            settingsRepo.setLanguage("SYSTEM")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        }
                        showLanguageSheet = false
                    }
                )
                LanguageOptionRow(
                    text = stringResource(R.string.lang_ru),
                    selected = language == "RU",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            settingsRepo.setLanguage("RU")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                        }
                        showLanguageSheet = false
                    }
                )
                LanguageOptionRow(
                    text = stringResource(R.string.lang_en),
                    selected = language == "EN",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            settingsRepo.setLanguage("EN")
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        }
                        showLanguageSheet = false
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pendingImportUri?.let { viewModel.importEntriesFromPdf(it) }
                    showImportConfirm = false
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.cancel))
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

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            ExpressiveSectionHeader(title = title, icon = icon)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SettingsInnerBox(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f))
    ) {
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f)
            )
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun LanguageOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}
