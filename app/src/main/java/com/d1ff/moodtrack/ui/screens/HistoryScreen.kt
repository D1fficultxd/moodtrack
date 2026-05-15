package com.d1ff.moodtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.DailyEntry
import com.d1ff.moodtrack.ui.components.GlassCard
import com.d1ff.moodtrack.ui.components.RoundedDateField
import com.d1ff.moodtrack.util.ExportUtils
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MoodViewModel = viewModel(),
    onNavigateToEdit: (String) -> Unit
) {
    val entries by viewModel.allEntries.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.calendar_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showExportDialog = true
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.tab_export))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    MonthSelector(currentMonth) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentMonth = it
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CalendarGrid(
                        month = currentMonth,
                        entries = entries,
                        onDateClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToEdit(it.toString())
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Legend()
                }
            }
        }
    }

    if (showExportDialog) {
        ExportRangeDialog(
            onDismiss = { showExportDialog = false },
            onExport = { start, end ->
                showExportDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val filtered = entries.filter { 
                    val d = LocalDate.parse(it.date)
                    (d.isEqual(start) || d.isAfter(start)) && (d.isEqual(end) || d.isBefore(end))
                }
                if (filtered.isEmpty()) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.no_entries_period)) }
                } else {
                    scope.launch {
                        val file = ExportUtils.generatePdf(context, filtered, start, end)
                        if (file != null) {
                            ExportUtils.sharePdf(context, file)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MonthSelector(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }
        
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}".replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        FilledTonalIconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun CalendarGrid(month: YearMonth, entries: List<DailyEntry>, onDateClick: (LocalDate) -> Unit) {
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfMonth = month.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val daysBefore = firstDayOfMonth - 1
    val today = remember { LocalDate.now() }
    val entriesByDate = remember(entries) { entries.associateBy { it.date } }
    
    val locale = Locale.getDefault()
    val weekDays = remember(locale.language) {
        if (locale.language == "ru") {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        } else {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        }
    }
    
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val totalCells = daysInMonth + daysBefore
        val rows = (totalCells + 6) / 7
        
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - daysBefore + 1
                    
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..daysInMonth) {
                            val date = month.atDay(day)
                            val entry = entriesByDate[date.toString()]
                            val isToday = date == today
                            val isFuture = date.isAfter(today)
                            
                            DayCell(date, entry, isToday, isFuture, onDateClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    entry: DailyEntry?,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: (LocalDate) -> Unit
) {
    val backgroundColor = when {
        isFuture -> Color.Transparent
        isToday -> MaterialTheme.colorScheme.primaryContainer
        entry != null -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        entry != null -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = { onClick(date) },
        enabled = !isFuture,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxSize()
            .alpha(if (isFuture) 0.38f else 1f),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = if (isToday && !isFuture) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        ) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isToday || entry != null) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor
                )
                if (entry != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val riskColor = if (entry.anxiety > 5 || entry.mood < 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        val urgentColor = if (entry.selfHarm || entry.suicidalThoughts > 0) MaterialTheme.colorScheme.error else riskColor
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(urgentColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.legend),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
            Text(
                " " + stringResource(R.string.legend_today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer))
            Text(
                " " + stringResource(R.string.legend_entry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
            Text(
                " " + stringResource(R.string.legend_risk),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ExportRangeDialog(onDismiss: () -> Unit, onExport: (LocalDate, LocalDate) -> Unit) {
    var start by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var end by remember { mutableStateOf(LocalDate.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.export_period))
                RoundedDateField(
                    label = stringResource(R.string.export_from),
                    date = start,
                    onDateChange = { start = it },
                    modifier = Modifier.fillMaxWidth()
                )
                RoundedDateField(
                    label = stringResource(R.string.export_to),
                    date = end,
                    onDateChange = { end = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onExport(start, end) }) { Text(stringResource(R.string.generate_pdf)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
