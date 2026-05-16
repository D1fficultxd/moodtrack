package com.d1ff.moodtrack.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.DailyEntry
import com.d1ff.moodtrack.data.SettingsRepository
import com.d1ff.moodtrack.health.HealthConnectAvailability
import com.d1ff.moodtrack.health.HealthConnectSleepManager
import com.d1ff.moodtrack.health.SleepImportResult
import com.d1ff.moodtrack.ui.components.ExpressiveSectionHeader
import com.d1ff.moodtrack.ui.components.GlassCard
import com.d1ff.moodtrack.ui.components.MetricLabelWithHelp
import com.d1ff.moodtrack.ui.components.MetricSlider
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class SaveStatus {
    IDLE, SAVING, SAVED, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: MoodViewModel = viewModel(),
    initialDate: String? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToGuide: () -> Unit
) {
    val dateToLoad = initialDate ?: LocalDate.now().toString()
    val entries by viewModel.allEntries.collectAsState()
    val entriesByDate = remember(entries) { entries.associateBy { it.date } }
    val entryForDate = remember(entriesByDate, dateToLoad) { entriesByDate[dateToLoad] }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val healthAutoFill by settingsRepository.healthConnectAutoFill.collectAsState(initial = false)
    val healthAskBeforeReplace by settingsRepository.healthConnectAskBeforeReplace.collectAsState(initial = true)
    val healthSleepManager = remember { HealthConnectSleepManager(context) }
    var healthRefreshTick by remember { mutableIntStateOf(0) }
    var pendingHealthSleep by remember(dateToLoad) { mutableStateOf<SleepImportResult.Success?>(null) }
    var healthSleepMessage by remember(dateToLoad) { mutableStateOf<String?>(null) }

    // Metrics State
    var sleepHours by remember(entryForDate, dateToLoad) { mutableFloatStateOf(entryForDate?.sleepHours ?: 8f) }
    var sleepEase by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.sleepEase ?: 0) }
    var anxiety by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.anxiety ?: 0) }
    var irritability by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.irritability ?: 0) }
    var impulsivity by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.impulsivity ?: 0) }
    var racingThoughts by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.racingThoughts ?: 0) }
    
    var mood by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.mood ?: 5) }
    var apathy by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.apathy ?: 0) }
    var fatigue by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.fatigue ?: 0) }
    var lossOfInterest by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.lossOfInterest ?: 0) }
    var hopelessness by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.hopelessness ?: 0) }
    
    var suicidalThoughts by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.suicidalThoughts ?: 0) }
    var selfHarm by remember(entryForDate, dateToLoad) { mutableStateOf(entryForDate?.selfHarm ?: false) }
    var note by remember(entryForDate, dateToLoad) { mutableStateOf(entryForDate?.note ?: "") }

    var helpDialogTitle by remember { mutableStateOf("") }
    var helpDialogContent by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    var depressiveExpanded by rememberSaveable { mutableStateOf(false) }
    var racingExpanded by rememberSaveable { mutableStateOf(false) }

    var saveStatus by remember { mutableStateOf(SaveStatus.IDLE) }
    val todayString = remember { LocalDate.now().toString() }
    val parsedDate = remember(dateToLoad) { LocalDate.parse(dateToLoad) }
    val titleFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy") }

    val isHighRisk = suicidalThoughts == 3 || selfHarm

    // Debounced Autosave Logic
    LaunchedEffect(
        sleepHours, sleepEase, anxiety, irritability, impulsivity, racingThoughts,
        mood, apathy, fatigue, lossOfInterest, hopelessness,
        suicidalThoughts, selfHarm, note
    ) {
        // Skip first emission when entry is just loaded
        if (entryForDate != null && 
            sleepHours == entryForDate.sleepHours &&
            sleepEase == entryForDate.sleepEase &&
            anxiety == entryForDate.anxiety &&
            irritability == entryForDate.irritability &&
            impulsivity == entryForDate.impulsivity &&
            racingThoughts == entryForDate.racingThoughts &&
            mood == entryForDate.mood &&
            apathy == entryForDate.apathy &&
            fatigue == entryForDate.fatigue &&
            lossOfInterest == entryForDate.lossOfInterest &&
            hopelessness == entryForDate.hopelessness &&
            suicidalThoughts == entryForDate.suicidalThoughts &&
            selfHarm == entryForDate.selfHarm &&
            note == entryForDate.note
        ) {
            return@LaunchedEffect
        }

        saveStatus = SaveStatus.SAVING
        delay(800) // Debounce delay
        
        try {
            viewModel.saveEntry(
                DailyEntry(
                    date = dateToLoad,
                    sleepHours = sleepHours,
                    sleepEase = sleepEase,
                    anxiety = anxiety,
                    irritability = irritability,
                    impulsivity = impulsivity,
                    racingThoughts = racingThoughts,
                    mood = mood,
                    apathy = apathy,
                    fatigue = fatigue,
                    lossOfInterest = lossOfInterest,
                    hopelessness = hopelessness,
                    suicidalThoughts = suicidalThoughts,
                    selfHarm = selfHarm,
                    note = note
                )
            )
            saveStatus = SaveStatus.SAVED
            delay(2000)
            if (saveStatus == SaveStatus.SAVED) saveStatus = SaveStatus.IDLE
        } catch (e: Exception) {
            e.printStackTrace()
            saveStatus = SaveStatus.ERROR
        }
    }

    LaunchedEffect(
        dateToLoad,
        healthAutoFill,
        healthAskBeforeReplace,
        entryForDate?.id,
        healthRefreshTick
    ) {
        pendingHealthSleep = null
        healthSleepMessage = null
        if (!healthAutoFill) return@LaunchedEffect

        when (val result = healthSleepManager.readSleepHours(parsedDate)) {
            is SleepImportResult.Success -> {
                val existingSleep = entryForDate?.sleepHours
                val shouldApplyAutomatically = entryForDate == null ||
                    existingSleep == null ||
                    existingSleep <= 0f ||
                    !healthAskBeforeReplace

                if (shouldApplyAutomatically) {
                    if (abs(sleepHours - result.hours) >= 0.25f) {
                        sleepHours = result.hours
                        snackbarHostState.showSnackbar(context.getString(R.string.health_connect_sleep_filled))
                    }
                } else if (existingSleep != null && abs(existingSleep - result.hours) >= 0.25f) {
                    pendingHealthSleep = result
                }
            }

            SleepImportResult.NoPermission -> {
                healthSleepMessage = context.getString(R.string.health_connect_no_sleep_permission)
            }

            SleepImportResult.NoData -> {
                healthSleepMessage = context.getString(R.string.health_connect_no_sleep_data)
            }

            SleepImportResult.Unavailable -> {
                healthSleepMessage = context.getString(R.string.health_connect_unavailable)
            }

            is SleepImportResult.Error -> {
                healthSleepMessage = context.getString(R.string.health_connect_read_error)
            }
        }
    }

    if (showHelpDialog) {
        ModalBottomSheet(
            onDismissRequest = { showHelpDialog = false },
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = helpDialogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    helpDialogContent.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = { showHelpDialog = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }

    // Helper to update help dialog
    val updateHelp: (Int, List<Int>) -> Unit = { titleRes, contentResList ->
        helpDialogTitle = context.getString(titleRes)
        helpDialogContent = contentResList.map { context.getString(it) }
        showHelpDialog = true
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (grantedPermissions.contains(healthSleepManager.sleepPermission)) {
            healthRefreshTick++
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.health_connect_no_sleep_permission))
            }
        }
    }

    val resetForm = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        sleepHours = 8f
        sleepEase = 0
        anxiety = 0
        irritability = 0
        impulsivity = 0
        racingThoughts = 0
        mood = 5
        apathy = 0
        fatigue = 0
        lossOfInterest = 0
        hopelessness = 0
        suicidalThoughts = 0
        selfHarm = false
        note = ""
    }

    val saveNow = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.saveEntry(
            DailyEntry(
                date = dateToLoad,
                sleepHours = sleepHours,
                sleepEase = sleepEase,
                anxiety = anxiety,
                irritability = irritability,
                impulsivity = impulsivity,
                racingThoughts = racingThoughts,
                mood = mood,
                apathy = apathy,
                fatigue = fatigue,
                lossOfInterest = lossOfInterest,
                hopelessness = hopelessness,
                suicidalThoughts = suicidalThoughts,
                selfHarm = selfHarm,
                note = note
            )
        )
        saveStatus = SaveStatus.SAVED
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.entry_saved))
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (dateToLoad == todayString) stringResource(R.string.today_title) else parsedDate.format(titleFormatter),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Crossfade(targetState = saveStatus, label = "SaveStatus") { status ->
                            val statusText = when (status) {
                                SaveStatus.SAVING -> stringResource(R.string.status_saving)
                                SaveStatus.SAVED -> stringResource(R.string.status_saved)
                                SaveStatus.ERROR -> stringResource(R.string.status_save_error)
                                else -> ""
                            }
                            if (statusText.isNotEmpty()) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (status == SaveStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToGuide()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.guide_title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (onBack != null) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isHighRisk) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = stringResource(R.string.risk_warning),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.section_core_metrics), Icons.Default.Insights) {
                MetricSlider(
                    title = stringResource(R.string.sleep_hours_label), 
                    value = sleepHours, 
                    onValueChange = { sleepHours = it },
                    range = 0f..16f,
                    steps = 31,
                    isInteger = false,
                    label = getSleepLabel(sleepHours),
                    onHelpClick = {
                        updateHelp(R.string.guide_sleep_title, listOf(
                            R.string.guide_sleep_8_10,
                            R.string.guide_sleep_7_8,
                            R.string.guide_sleep_6_7,
                            R.string.guide_sleep_5_6,
                            R.string.guide_sleep_lt5,
                            R.string.guide_sleep_lt4_no_fatigue
                        ))
                    }
                )

                pendingHealthSleep?.let { sleepResult ->
                    HealthSleepSuggestionCard(
                        message = stringResource(
                            if (sleepResult.usedSessionFallback) {
                                R.string.health_connect_sleep_period_found
                            } else {
                                R.string.health_connect_sleep_found
                            },
                            formatHealthSleepDuration(sleepResult.minutes)
                        ),
                        savedAs = stringResource(
                            R.string.health_connect_sleep_will_save,
                            formatHealthSleepHours(sleepResult.hours)
                        ),
                        caption = if (sleepResult.usedSessionFallback) {
                            stringResource(R.string.health_connect_sleep_fallback_note)
                        } else {
                            null
                        },
                        onUse = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sleepHours = sleepResult.hours
                            pendingHealthSleep = null
                            healthSleepMessage = null
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.health_connect_sleep_filled))
                            }
                        },
                        onKeepManual = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pendingHealthSleep = null
                        }
                    )
                }

                if (pendingHealthSleep == null && healthSleepMessage != null) {
                    HealthSleepStatusCard(
                        message = healthSleepMessage.orEmpty(),
                        actionText = if (healthSleepMessage == stringResource(R.string.health_connect_no_sleep_permission)) {
                            stringResource(R.string.health_connect_connect)
                        } else {
                            null
                        },
                        onAction = {
                            when (healthSleepManager.availability()) {
                                HealthConnectAvailability.Available -> healthPermissionLauncher.launch(healthSleepManager.permissions)
                                HealthConnectAvailability.InstallOrUpdateRequired -> {
                                    runCatching { context.startActivity(healthSleepManager.installIntent()) }
                                }
                                HealthConnectAvailability.Unavailable -> Unit
                            }
                        }
                    )
                }

                MetricSlider(
                    title = stringResource(R.string.mood_label), 
                    value = mood.toFloat(), 
                    onValueChange = { mood = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getMoodLabel(mood),
                    onHelpClick = {
                        updateHelp(R.string.guide_mood_title, listOf(
                            R.string.guide_mood_0_2,
                            R.string.guide_mood_3_4,
                            R.string.guide_mood_5,
                            R.string.guide_mood_6_7,
                            R.string.guide_mood_8,
                            R.string.guide_mood_9_10,
                            R.string.guide_mood_warning
                        ))
                    }
                )

                MetricSlider(
                    title = stringResource(R.string.anxiety_label), 
                    value = anxiety.toFloat(), 
                    onValueChange = { anxiety = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(anxiety),
                    onHelpClick = {
                        updateHelp(R.string.guide_anxiety_title, listOf(
                            R.string.guide_anxiety_0,
                            R.string.guide_anxiety_1_2,
                            R.string.guide_anxiety_3_4,
                            R.string.guide_anxiety_5_6,
                            R.string.guide_anxiety_7_8,
                            R.string.guide_anxiety_9_10
                        ))
                    }
                )
                }
            }

            // Expandable Sections
            item {
                ExpandableSectionCard(
                title = stringResource(R.string.section_depressive_expand),
                icon = Icons.Default.SentimentVeryDissatisfied,
                expanded = depressiveExpanded,
                onExpandClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    depressiveExpanded = !depressiveExpanded
                }
                ) {
                MetricSlider(
                    title = stringResource(R.string.apathy_label), 
                    value = apathy.toFloat(), 
                    onValueChange = { apathy = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(apathy),
                    onHelpClick = {
                        updateHelp(R.string.guide_apathy_title, listOf(
                            R.string.guide_apathy_0,
                            R.string.guide_apathy_1_2,
                            R.string.guide_apathy_3_4,
                            R.string.guide_apathy_5_6,
                            R.string.guide_apathy_7_8,
                            R.string.guide_apathy_9_10
                        ))
                    }
                )
                MetricSlider(
                    title = stringResource(R.string.fatigue_label), 
                    value = fatigue.toFloat(), 
                    onValueChange = { fatigue = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(fatigue),
                    onHelpClick = {
                        updateHelp(R.string.guide_fatigue_title, listOf(
                            R.string.guide_fatigue_0,
                            R.string.guide_fatigue_1_2,
                            R.string.guide_fatigue_3_4,
                            R.string.guide_fatigue_5_6,
                            R.string.guide_fatigue_7_8,
                            R.string.guide_fatigue_9_10
                        ))
                    }
                )
                MetricSlider(
                    title = stringResource(R.string.loss_of_interest_label), 
                    value = lossOfInterest.toFloat(), 
                    onValueChange = { lossOfInterest = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(lossOfInterest),
                    onHelpClick = {
                        updateHelp(R.string.guide_loss_of_interest_title, listOf(
                            R.string.guide_loss_of_interest_0,
                            R.string.guide_loss_of_interest_1_2,
                            R.string.guide_loss_of_interest_3_4,
                            R.string.guide_loss_of_interest_5_6,
                            R.string.guide_loss_of_interest_7_8,
                            R.string.guide_loss_of_interest_9_10
                        ))
                    }
                )
                MetricSlider(
                    title = stringResource(R.string.hopelessness_label), 
                    value = hopelessness.toFloat(), 
                    onValueChange = { hopelessness = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(hopelessness),
                    onHelpClick = {
                        updateHelp(R.string.guide_hopelessness_title, listOf(
                            R.string.guide_hopelessness_0,
                            R.string.guide_hopelessness_1_2,
                            R.string.guide_hopelessness_3_4,
                            R.string.guide_hopelessness_5_6,
                            R.string.guide_hopelessness_7_8,
                            R.string.guide_hopelessness_9_10
                        ))
                    }
                )
                }
            }

            item {
                ExpandableSectionCard(
                title = stringResource(R.string.section_racing_expand),
                icon = Icons.Default.Bolt,
                expanded = racingExpanded,
                onExpandClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    racingExpanded = !racingExpanded
                }
                ) {
                MetricSlider(
                    title = stringResource(R.string.irritability_label), 
                    value = irritability.toFloat(), 
                    onValueChange = { irritability = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(irritability),
                    onHelpClick = {
                        updateHelp(R.string.guide_irritability_title, listOf(
                            R.string.guide_irritability_0,
                            R.string.guide_irritability_1_2,
                            R.string.guide_irritability_3_4,
                            R.string.guide_irritability_5_6,
                            R.string.guide_irritability_7_8,
                            R.string.guide_irritability_9_10
                        ))
                    }
                )
                MetricSlider(
                    title = stringResource(R.string.impulsivity_label), 
                    value = impulsivity.toFloat(), 
                    onValueChange = { impulsivity = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(impulsivity),
                    onHelpClick = {
                        updateHelp(R.string.guide_impulsivity_title, listOf(
                            R.string.guide_impulsivity_0,
                            R.string.guide_impulsivity_1_2,
                            R.string.guide_impulsivity_3_4,
                            R.string.guide_impulsivity_5_6,
                            R.string.guide_impulsivity_7_8,
                            R.string.guide_impulsivity_9_10
                        ))
                    }
                )
                MetricSlider(
                    title = stringResource(R.string.racing_thoughts_label), 
                    value = racingThoughts.toFloat(), 
                    onValueChange = { racingThoughts = it.toInt() }, 
                    range = 0f..10f, 
                    steps = 9,
                    label = getSymptomLabel(racingThoughts),
                    onHelpClick = {
                        updateHelp(R.string.guide_racing_thoughts_title, listOf(
                            R.string.guide_racing_thoughts_0,
                            R.string.guide_racing_thoughts_1_2,
                            R.string.guide_racing_thoughts_3_4,
                            R.string.guide_racing_thoughts_5_6,
                            R.string.guide_racing_thoughts_7_8,
                            R.string.guide_racing_thoughts_9_10
                        ))
                    }
                )
                }
            }

            item {
                SectionCard(stringResource(R.string.section_risks), Icons.Default.Warning) {
                MetricLabelWithHelp(
                    title = stringResource(R.string.suicidal_thoughts_label),
                    textStyle = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    onHelpClick = {
                        updateHelp(R.string.guide_suicidal_title, listOf(
                            R.string.guide_suicidal_0,
                            R.string.guide_suicidal_1,
                            R.string.guide_suicidal_2,
                            R.string.guide_suicidal_3
                        ))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                val suicidalNone = stringResource(R.string.suicidal_none)
                val suicidalPassive = stringResource(R.string.suicidal_passive)
                val suicidalFrequent = stringResource(R.string.suicidal_frequent)
                val suicidalHigh = stringResource(R.string.suicidal_high)
                val suicidalOptions = remember(
                    suicidalNone,
                    suicidalPassive,
                    suicidalFrequent,
                    suicidalHigh
                ) {
                    listOf(suicidalNone, suicidalPassive, suicidalFrequent, suicidalHigh)
                }
                suicidalOptions.forEachIndexed { index, text ->
                    SuicidalOptionRow(
                        text = text,
                        selected = suicidalThoughts == index,
                        onClick = {
                            if (suicidalThoughts != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                suicidalThoughts = index
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                SelfHarmRow(
                    checked = selfHarm,
                    onToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selfHarm = !selfHarm
                    },
                    onHelpClick = {
                            updateHelp(R.string.guide_self_harm_title, listOf(
                                R.string.guide_self_harm_desc,
                                R.string.guide_self_harm_rule
                            ))
                    }
                )
                }
            }

            item {
                SectionCard(stringResource(R.string.section_note), Icons.AutoMirrored.Filled.Note) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                }
            }

            item {
                TodayActionButtons(
                    isExistingEntry = entryForDate != null,
                    onClear = { resetForm() },
                    onSave = { saveNow() }
                )
            }
        }
    }
}

@Composable
private fun SuicidalOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(22.dp),
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
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.secondary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SelfHarmRow(
    checked: Boolean,
    onToggle: () -> Unit,
    onHelpClick: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (checked) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.30f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            MetricLabelWithHelp(
                title = stringResource(R.string.self_harm),
                onHelpClick = onHelpClick,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun TodayActionButtons(
    isExistingEntry: Boolean,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.clear))
        }
        FilledTonalButton(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (isExistingEntry) R.string.update else R.string.save))
        }
    }
}

@Composable
private fun HealthSleepSuggestionCard(
    message: String,
    savedAs: String,
    caption: String?,
    onUse: () -> Unit,
    onKeepManual: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = savedAs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.82f)
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.72f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onKeepManual) {
                    Text(stringResource(R.string.health_connect_sleep_keep_manual))
                }
                FilledTonalButton(
                    onClick = onUse,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.health_connect_sleep_use))
                }
            }
        }
    }
}

@Composable
private fun HealthSleepStatusCard(
    message: String,
    actionText: String?,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionText != null) {
                TextButton(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}

private fun formatHealthSleepDuration(minutes: Long): String {
    val hours = minutes / 60
    val restMinutes = minutes % 60
    return if (Locale.getDefault().language == "ru") {
        "${hours} ч ${restMinutes} мин"
    } else {
        "${hours} h ${restMinutes} min"
    }
}

private fun formatHealthSleepHours(hours: Float): String {
    val formatted = String.format(Locale.getDefault(), "%.1f", hours)
    return if (Locale.getDefault().language == "ru") {
        "$formatted ч"
    } else {
        "$formatted h"
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            ExpressiveSectionHeader(title = title, icon = icon)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ExpandableSectionCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                onClick = onExpandClick,
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveSectionHeader(title = title, icon = icon, modifier = Modifier.weight(1f))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                Spacer(modifier = Modifier.height(12.dp))
                content()
                }
            }
        }
    }
}

@Composable
fun getSymptomLabel(value: Int): String {
    return when (value) {
        0 -> stringResource(R.string.label_none)
        1, 2 -> stringResource(R.string.label_weak)
        3, 4 -> stringResource(R.string.label_noticeable)
        5, 6 -> stringResource(R.string.label_interferes)
        7, 8 -> stringResource(R.string.label_strong)
        else -> stringResource(R.string.label_critical)
    }
}

@Composable
fun getMoodLabel(value: Int): String {
    return when (value) {
        0, 1, 2 -> stringResource(R.string.label_mood_very_bad)
        3, 4 -> stringResource(R.string.label_mood_low)
        5 -> stringResource(R.string.label_mood_neutral)
        6, 7 -> stringResource(R.string.label_mood_good)
        8 -> stringResource(R.string.label_mood_elevated)
        else -> stringResource(R.string.label_mood_racing)
    }
}

@Composable
fun getSleepLabel(hours: Float): String {
    return when {
        hours >= 8f && hours <= 10f -> stringResource(R.string.label_sleep_normal)
        hours > 7f -> stringResource(R.string.label_sleep_good)
        hours > 6f -> stringResource(R.string.label_sleep_tolerable)
        hours > 5f -> stringResource(R.string.label_sleep_risk)
        hours < 4f -> stringResource(R.string.label_sleep_racing_marker)
        else -> stringResource(R.string.label_sleep_red_flag)
    }
}
