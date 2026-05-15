package com.d1ff.moodtrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.DailyEntry
import com.d1ff.moodtrack.ui.components.ExpressiveSectionHeader
import com.d1ff.moodtrack.ui.components.GlassCard
import com.d1ff.moodtrack.ui.components.MetricSlider
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val entryForDate = remember(entries, dateToLoad) {
        entries.find { it.date == dateToLoad }
    }
    
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

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
    var actionsExpanded by remember { mutableStateOf(false) }

    var saveStatus by remember { mutableStateOf(SaveStatus.IDLE) }

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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(helpDialogTitle) },
            text = {
                Column {
                    helpDialogContent.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Helper to update help dialog
    val updateHelp: (Int, List<Int>) -> Unit = { titleRes, contentResList ->
        helpDialogTitle = context.getString(titleRes)
        helpDialogContent = contentResList.map { context.getString(it) }
        showHelpDialog = true
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
                        val parsedDate = LocalDate.parse(dateToLoad)
                        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
                        Text(
                            text = if (dateToLoad == LocalDate.now().toString()) stringResource(R.string.today_title) else parsedDate.format(formatter),
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.guide_title)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actionsExpanded = true
                            }
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = actionsExpanded,
                            onDismissRequest = { actionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (entryForDate != null) R.string.update else R.string.save)) },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                                onClick = {
                                    actionsExpanded = false
                                    saveNow()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear)) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    actionsExpanded = false
                                    resetForm()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (onBack != null) Modifier.navigationBarsPadding() else Modifier)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isHighRisk) {
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

            // Main Section
            SectionCard(stringResource(R.string.section_sleep), Icons.Default.Bed) {
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
                MetricSlider(
                    title = stringResource(R.string.sleep_ease_label), 
                    value = sleepEase.toFloat(), 
                    onValueChange = { sleepEase = it.toInt() },
                    range = 0f..10f,
                    steps = 9,
                    label = getEaseLabel(sleepEase),
                    onHelpClick = {
                        updateHelp(R.string.guide_sleep_ease_title, listOf(
                            R.string.guide_sleep_ease_0,
                            R.string.guide_sleep_ease_1_2,
                            R.string.guide_sleep_ease_3_4,
                            R.string.guide_sleep_ease_5_6,
                            R.string.guide_sleep_ease_7_8,
                            R.string.guide_sleep_ease_9_10
                        ))
                    }
                )
            }

            SectionCard(stringResource(R.string.mood_label), Icons.Default.SentimentSatisfied) {
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
            }

            SectionCard(stringResource(R.string.anxiety_label), Icons.Default.Psychology) {
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

            // Expandable Sections
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

            SectionCard(stringResource(R.string.section_risks), Icons.Default.Warning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.suicidal_thoughts_label), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = {
                        updateHelp(R.string.guide_suicidal_title, listOf(
                            R.string.guide_suicidal_0,
                            R.string.guide_suicidal_1,
                            R.string.guide_suicidal_2,
                            R.string.guide_suicidal_3
                        ))
                    }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                val suicidalOptions = listOf(
                    stringResource(R.string.suicidal_none),
                    stringResource(R.string.suicidal_passive),
                    stringResource(R.string.suicidal_frequent),
                    stringResource(R.string.suicidal_high)
                )
                suicidalOptions.forEachIndexed { index, text ->
                    val isSelected = suicidalThoughts == index
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            suicidalThoughts = index
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                            }
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null // Click handled by parent Surface
                            )
                            Text(
                                text = text,
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selfHarm = !selfHarm
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (selfHarm) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selfHarm) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.30f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.self_harm),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selfHarm) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = {
                            updateHelp(R.string.guide_self_harm_title, listOf(
                                R.string.guide_self_harm_desc,
                                R.string.guide_self_harm_rule
                            ))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Switch(
                            checked = selfHarm,
                            onCheckedChange = null // Click handled by parent Surface
                        )
                    }
                }
            }

            SectionCard(stringResource(R.string.section_note), Icons.AutoMirrored.Filled.Note) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MaterialTheme.shapes.large
                )
            }

        }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveSectionHeader(title = title, icon = icon, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
fun getEaseLabel(value: Int): String {
    return when (value) {
        0 -> stringResource(R.string.guide_sleep_ease_0).split(" — ").last().lowercase()
        1, 2 -> stringResource(R.string.guide_sleep_ease_1_2).split(" — ").last().lowercase()
        3, 4 -> stringResource(R.string.guide_sleep_ease_3_4).split(" — ").last().lowercase()
        5, 6 -> stringResource(R.string.guide_sleep_ease_5_6).split(" — ").last().lowercase()
        7, 8 -> stringResource(R.string.guide_sleep_ease_7_8).split(" — ").last().lowercase()
        else -> stringResource(R.string.guide_sleep_ease_9_10).split(" — ").last().lowercase()
    }
}
