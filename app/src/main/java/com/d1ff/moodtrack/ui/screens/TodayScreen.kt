package com.d1ff.moodtrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.DailyEntry
import com.d1ff.moodtrack.ui.components.MetricSlider
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: MoodViewModel = viewModel(),
    initialDate: String? = null
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
    var sleepEase by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.sleepEase ?: 5) }
    var anxiety by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.anxiety ?: 0) }
    var irritability by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.irritability ?: 0) }
    var impulsivity by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.impulsivity ?: 0) }
    var racingThoughts by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.racingThoughts ?: 0) }
    
    // New Depressive Metrics
    var mood by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.mood ?: 5) }
    var apathy by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.apathy ?: 0) }
    var fatigue by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.fatigue ?: 0) }
    var lossOfInterest by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.lossOfInterest ?: 0) }
    var hopelessness by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.hopelessness ?: 0) }
    
    var suicidalThoughts by remember(entryForDate, dateToLoad) { mutableIntStateOf(entryForDate?.suicidalThoughts ?: 0) }
    var selfHarm by remember(entryForDate, dateToLoad) { mutableStateOf(entryForDate?.selfHarm ?: false) }
    var note by remember(entryForDate, dateToLoad) { mutableStateOf(entryForDate?.note ?: "") }

    val isHighRisk = suicidalThoughts == 3 || selfHarm

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val parsedDate = LocalDate.parse(dateToLoad)
                    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
                    Text(
                        text = if (dateToLoad == LocalDate.now().toString()) stringResource(R.string.today_title) else parsedDate.format(formatter),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            SectionCard(stringResource(R.string.section_sleep), Icons.Default.Bed) {
                MetricSlider(
                    title = stringResource(R.string.section_sleep), 
                    value = sleepHours, 
                    onValueChange = { sleepHours = it },
                    range = 0f..16f,
                    steps = 31,
                    isInteger = false
                )
                MetricSlider(
                    title = stringResource(R.string.sleep_ease).substringBefore(":"), 
                    value = sleepEase.toFloat(), 
                    onValueChange = { sleepEase = it.toInt() },
                    range = 0f..10f,
                    steps = 9
                )
            }

            SectionCard(stringResource(R.string.section_anxiety), Icons.Default.Psychology) {
                MetricSlider(title = stringResource(R.string.anxiety_label), value = anxiety.toFloat(), onValueChange = { anxiety = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.irritability).substringBefore(":"), value = irritability.toFloat(), onValueChange = { irritability = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.impulsivity).substringBefore(":"), value = impulsivity.toFloat(), onValueChange = { impulsivity = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.racing_thoughts).substringBefore(":"), value = racingThoughts.toFloat(), onValueChange = { racingThoughts = it.toInt() }, range = 0f..10f, steps = 9)
            }

            SectionCard(stringResource(R.string.section_depression), Icons.Default.SentimentDissatisfied) {
                MetricSlider(title = stringResource(R.string.mood_label), value = mood.toFloat(), onValueChange = { mood = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.apathy_label), value = apathy.toFloat(), onValueChange = { apathy = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.fatigue_label), value = fatigue.toFloat(), onValueChange = { fatigue = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.loss_of_interest).substringBefore(":"), value = lossOfInterest.toFloat(), onValueChange = { lossOfInterest = it.toInt() }, range = 0f..10f, steps = 9)
                MetricSlider(title = stringResource(R.string.hopelessness).substringBefore(":"), value = hopelessness.toFloat(), onValueChange = { hopelessness = it.toInt() }, range = 0f..10f, steps = 9)
            }

            SectionCard(stringResource(R.string.section_risks), Icons.Default.Warning) {
                Text(stringResource(R.string.suicidal_thoughts), style = MaterialTheme.typography.titleSmall)
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
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
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
                    shape = MaterialTheme.shapes.medium,
                    color = if (selfHarm) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sleepHours = 8f
                        sleepEase = 5
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
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(stringResource(R.string.clear))
                }
                Button(
                    onClick = {
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
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.entry_saved))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (entryForDate != null) R.string.update else R.string.save))
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
