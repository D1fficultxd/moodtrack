package com.d1ff.moodtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d1ff.moodtrack.R
import com.d1ff.moodtrack.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    BackHandler {
        onBack()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.large
            )

            val sections = listOf(
                GuideSection(
                    stringResource(R.string.guide_scale_title),
                    listOf(
                        stringResource(R.string.guide_scale_0),
                        stringResource(R.string.guide_scale_1_2),
                        stringResource(R.string.guide_scale_3_4),
                        stringResource(R.string.guide_scale_5_6),
                        stringResource(R.string.guide_scale_7_8),
                        stringResource(R.string.guide_scale_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_mood_title),
                    listOf(
                        stringResource(R.string.guide_mood_0_2),
                        stringResource(R.string.guide_mood_3_4),
                        stringResource(R.string.guide_mood_5),
                        stringResource(R.string.guide_mood_6_7),
                        stringResource(R.string.guide_mood_8),
                        stringResource(R.string.guide_mood_9_10),
                        stringResource(R.string.guide_mood_warning)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_sleep_title),
                    listOf(
                        stringResource(R.string.guide_sleep_8_10),
                        stringResource(R.string.guide_sleep_7_8),
                        stringResource(R.string.guide_sleep_6_7),
                        stringResource(R.string.guide_sleep_5_6),
                        stringResource(R.string.guide_sleep_lt5),
                        stringResource(R.string.guide_sleep_lt4_no_fatigue)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_sleep_ease_title),
                    listOf(
                        stringResource(R.string.guide_sleep_ease_0),
                        stringResource(R.string.guide_sleep_ease_1_2),
                        stringResource(R.string.guide_sleep_ease_3_4),
                        stringResource(R.string.guide_sleep_ease_5_6),
                        stringResource(R.string.guide_sleep_ease_7_8),
                        stringResource(R.string.guide_sleep_ease_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_anxiety_title),
                    listOf(
                        stringResource(R.string.guide_anxiety_0),
                        stringResource(R.string.guide_anxiety_1_2),
                        stringResource(R.string.guide_anxiety_3_4),
                        stringResource(R.string.guide_anxiety_5_6),
                        stringResource(R.string.guide_anxiety_7_8),
                        stringResource(R.string.guide_anxiety_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_irritability_title),
                    listOf(
                        stringResource(R.string.guide_irritability_0),
                        stringResource(R.string.guide_irritability_1_2),
                        stringResource(R.string.guide_irritability_3_4),
                        stringResource(R.string.guide_irritability_5_6),
                        stringResource(R.string.guide_irritability_7_8),
                        stringResource(R.string.guide_irritability_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_impulsivity_title),
                    listOf(
                        stringResource(R.string.guide_impulsivity_0),
                        stringResource(R.string.guide_impulsivity_1_2),
                        stringResource(R.string.guide_impulsivity_3_4),
                        stringResource(R.string.guide_impulsivity_5_6),
                        stringResource(R.string.guide_impulsivity_7_8),
                        stringResource(R.string.guide_impulsivity_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_racing_thoughts_title),
                    listOf(
                        stringResource(R.string.guide_racing_thoughts_0),
                        stringResource(R.string.guide_racing_thoughts_1_2),
                        stringResource(R.string.guide_racing_thoughts_3_4),
                        stringResource(R.string.guide_racing_thoughts_5_6),
                        stringResource(R.string.guide_racing_thoughts_7_8),
                        stringResource(R.string.guide_racing_thoughts_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_apathy_title),
                    listOf(
                        stringResource(R.string.guide_apathy_0),
                        stringResource(R.string.guide_apathy_1_2),
                        stringResource(R.string.guide_apathy_3_4),
                        stringResource(R.string.guide_apathy_5_6),
                        stringResource(R.string.guide_apathy_7_8),
                        stringResource(R.string.guide_apathy_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_fatigue_title),
                    listOf(
                        stringResource(R.string.guide_fatigue_0),
                        stringResource(R.string.guide_fatigue_1_2),
                        stringResource(R.string.guide_fatigue_3_4),
                        stringResource(R.string.guide_fatigue_5_6),
                        stringResource(R.string.guide_fatigue_7_8),
                        stringResource(R.string.guide_fatigue_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_loss_of_interest_title),
                    listOf(
                        stringResource(R.string.guide_loss_of_interest_0),
                        stringResource(R.string.guide_loss_of_interest_1_2),
                        stringResource(R.string.guide_loss_of_interest_3_4),
                        stringResource(R.string.guide_loss_of_interest_5_6),
                        stringResource(R.string.guide_loss_of_interest_7_8),
                        stringResource(R.string.guide_loss_of_interest_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_hopelessness_title),
                    listOf(
                        stringResource(R.string.guide_hopelessness_0),
                        stringResource(R.string.guide_hopelessness_1_2),
                        stringResource(R.string.guide_hopelessness_3_4),
                        stringResource(R.string.guide_hopelessness_5_6),
                        stringResource(R.string.guide_hopelessness_7_8),
                        stringResource(R.string.guide_hopelessness_9_10)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_suicidal_title),
                    listOf(
                        stringResource(R.string.guide_suicidal_0),
                        stringResource(R.string.guide_suicidal_1),
                        stringResource(R.string.guide_suicidal_2),
                        stringResource(R.string.guide_suicidal_3)
                    )
                ),
                GuideSection(
                    stringResource(R.string.guide_self_harm_title),
                    listOf(
                        stringResource(R.string.guide_self_harm_desc),
                        stringResource(R.string.guide_self_harm_rule)
                    )
                )
            )

            sections.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.items.any { item -> item.contains(searchQuery, ignoreCase = true) } 
            }.forEach { section ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        section.items.forEach { item ->
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class GuideSection(val title: String, val items: List<String>)
