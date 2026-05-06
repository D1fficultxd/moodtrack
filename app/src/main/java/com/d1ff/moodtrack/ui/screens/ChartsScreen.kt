package com.d1ff.moodtrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.data.DailyEntry
import com.d1ff.moodtrack.viewmodel.MoodViewModel

@Composable
fun ChartsScreen(viewModel: MoodViewModel = viewModel()) {
    val allEntries by viewModel.allEntries.collectAsState()
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    
    var selectedFilter by remember { mutableIntStateOf(30) } // 7, 14, 30, Int.MAX_VALUE
    
    val filteredEntries = remember(allEntries, selectedFilter) {
        val sorted = allEntries.sortedBy { it.date }
        if (selectedFilter == Int.MAX_VALUE) sorted else sorted.takeLast(selectedFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
    ) {
        Text(
            text = stringResource(R.string.charts_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyRow(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                7 to R.string.filter_7d,
                14 to R.string.filter_14d,
                30 to R.string.filter_30d,
                Int.MAX_VALUE to R.string.filter_all
            )
            items(filters) { (days, labelRes) ->
                FilterChip(
                    selected = selectedFilter == days,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFilter = days 
                    },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        
        if (filteredEntries.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filteredEntries.isEmpty()) stringResource(R.string.no_data_charts) else stringResource(R.string.need_more_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Column
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val errorColor = MaterialTheme.colorScheme.error
        
        ChartCard(stringResource(R.string.mood_label), filteredEntries.map { it.mood.toFloat() }, 10f, primaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.anxiety_label), filteredEntries.map { it.anxiety.toFloat() }, 10f, secondaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.sleep_hours, 0f).substringBefore(":"), filteredEntries.map { it.sleepHours }, 16f, tertiaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.apathy_label), filteredEntries.map { it.apathy.toFloat() }, 10f, errorColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.fatigue_label), filteredEntries.map { it.fatigue.toFloat() }, 10f, primaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.irritability).substringBefore(":"), filteredEntries.map { it.irritability.toFloat() }, 10f, secondaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.impulsivity).substringBefore(":"), filteredEntries.map { it.impulsivity.toFloat() }, 10f, tertiaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.racing_thoughts).substringBefore(":"), filteredEntries.map { it.racingThoughts.toFloat() }, 10f, errorColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.loss_of_interest).substringBefore(":"), filteredEntries.map { it.lossOfInterest.toFloat() }, 10f, primaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.hopelessness).substringBefore(":"), filteredEntries.map { it.hopelessness.toFloat() }, 10f, secondaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        ChartCard(stringResource(R.string.suicidal_thoughts).substringBefore(":"), filteredEntries.map { it.suicidalThoughts.toFloat() }, 3f, errorColor)
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ChartCard(title: String, data: List<Float>, maxValue: Float, color: Color) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            SimpleLineChart(data, maxValue, color)
        }
    }
}

@Composable
fun SimpleLineChart(data: List<Float>, maxValue: Float, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (data.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val xStep = width / (data.size - 1)
        
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * xStep
            val y = height - (value / maxValue * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        data.forEachIndexed { index, value ->
            val x = index * xStep
            val y = height - (value / maxValue * height)
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
