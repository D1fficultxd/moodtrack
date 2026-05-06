package com.d1ff.moodtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d1ff.moodtrack.R
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
    var isExporting by remember { mutableStateOf(false) }
    
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.export_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.export_period), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = startDate.toString(),
                        onValueChange = { try { startDate = LocalDate.parse(it) } catch(e:Exception){} },
                        label = { Text(stringResource(R.string.export_from)) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = endDate.toString(),
                        onValueChange = { try { endDate = LocalDate.parse(it) } catch(e:Exception){} },
                        label = { Text(stringResource(R.string.export_to)) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.export_description),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                val filtered = allEntries.filter { 
                    val d = LocalDate.parse(it.date)
                    (d.isEqual(startDate) || d.isAfter(startDate)) && (d.isEqual(endDate) || d.isBefore(endDate))
                }
                if (filtered.isNotEmpty()) {
                    isExporting = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        val file = ExportUtils.generatePdf(context, filtered, startDate, endDate)
                        isExporting = false
                        if (file != null) {
                            ExportUtils.sharePdf(context, file)
                        }
                    }
                }
            },
            enabled = allEntries.isNotEmpty() && !isExporting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.generate_pdf))
            }
        }
        
        if (allEntries.isEmpty()) {
            Text(stringResource(R.string.no_data_export), color = MaterialTheme.colorScheme.error)
        }
    }
}
