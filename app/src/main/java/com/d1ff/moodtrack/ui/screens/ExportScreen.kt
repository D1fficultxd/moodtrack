package com.d1ff.moodtrack.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.d1ff.moodtrack.ui.components.ExpressiveSectionHeader
import com.d1ff.moodtrack.ui.components.GlassCard
import com.d1ff.moodtrack.ui.components.RoundedDateField
import com.d1ff.moodtrack.util.ExportUtils
import com.d1ff.moodtrack.viewmodel.MoodViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ExportScreen(viewModel: MoodViewModel = viewModel()) {
    val allEntries by viewModel.allEntries.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isGeneratingRegularPdf by remember { mutableStateOf(false) }
    var isGeneratingRatingGuide by remember { mutableStateOf(false) }
    var isGeneratingDoctorReport by remember { mutableStateOf(false) }
    var includeRatingGuide by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    val entriesWithDates = remember(allEntries) {
        allEntries.mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { date -> date to entry }
        }
    }
    val entriesForPeriod = remember(entriesWithDates, startDate, endDate) {
        entriesWithDates
            .filter { (date, _) ->
                (date.isEqual(startDate) || date.isAfter(startDate)) &&
                    (date.isEqual(endDate) || date.isBefore(endDate))
            }
            .map { it.second }
    }
    val anyExportRunning = isGeneratingRegularPdf || isGeneratingRatingGuide || isGeneratingDoctorReport

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.export_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExpressiveSectionHeader(
                        title = stringResource(R.string.export_period),
                        icon = Icons.Default.DateRange
                    )

                    RoundedDateField(
                        label = stringResource(R.string.export_from),
                        date = startDate,
                        onDateChange = { startDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    RoundedDateField(
                        label = stringResource(R.string.export_to),
                        date = endDate,
                        onDateChange = { endDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ExpressiveSectionHeader(
                        title = stringResource(R.string.export_include_rating_guide),
                        icon = Icons.Default.Tune
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.export_include_rating_guide),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = includeRatingGuide,
                            onCheckedChange = {
                                includeRatingGuide = it
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpressiveSectionHeader(
                        title = stringResource(R.string.generate_pdf),
                        icon = Icons.Default.PictureAsPdf
                    )

                    Button(
                        onClick = {
                            if (anyExportRunning) return@Button
                            isGeneratingRegularPdf = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                try {
                                    val file = ExportUtils.generatePdf(context, entriesForPeriod, startDate, endDate, includeRatingGuide)
                                    if (file != null) {
                                        ExportUtils.sharePdf(context, file)
                                    }
                                } finally {
                                    isGeneratingRegularPdf = false
                                }
                            }
                        },
                        enabled = !anyExportRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        if (isGeneratingRegularPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.generate_pdf))
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            if (anyExportRunning) return@FilledTonalButton
                            isGeneratingRatingGuide = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                try {
                                    val file = ExportUtils.generateRatingGuidePdf(context)
                                    if (file != null) {
                                        ExportUtils.sharePdf(context, file)
                                    }
                                } finally {
                                    isGeneratingRatingGuide = false
                                }
                            }
                        },
                        enabled = !anyExportRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        if (isGeneratingRatingGuide) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_rating_guide))
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpressiveSectionHeader(
                        title = stringResource(R.string.export_doctor_title),
                        icon = Icons.Default.MedicalServices
                    )

                    Text(
                        text = stringResource(R.string.export_doctor_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = {
                            if (anyExportRunning) return@FilledTonalButton
                            val doctorEndDate = LocalDate.now()
                            val doctorStartDate = doctorEndDate.minusDays(13)
                            val doctorEntries = entriesWithDates
                                .filter { (date, _) ->
                                    (date.isEqual(doctorStartDate) || date.isAfter(doctorStartDate)) &&
                                        (date.isEqual(doctorEndDate) || date.isBefore(doctorEndDate))
                                }
                                .map { it.second }

                            if (doctorEntries.isEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.export_doctor_empty))
                                }
                                return@FilledTonalButton
                            }
                            isGeneratingDoctorReport = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                try {
                                    val file = ExportUtils.generateDoctorPdf(
                                        context = context,
                                        entries = doctorEntries,
                                        start = doctorStartDate,
                                        end = doctorEndDate
                                    )
                                    if (file != null) {
                                        ExportUtils.sharePdf(context, file)
                                    }
                                } finally {
                                    isGeneratingDoctorReport = false
                                }
                            }
                        },
                        enabled = !anyExportRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        if (isGeneratingDoctorReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.MedicalServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_doctor_title), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (allEntries.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.24f))
                ) {
                    Text(
                        text = stringResource(R.string.no_data_export),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    }
}
