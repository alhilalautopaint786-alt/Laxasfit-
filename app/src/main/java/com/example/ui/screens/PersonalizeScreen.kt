package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.catalog.BeginnerYogaCatalog
import com.example.data.model.UserProfile
import com.example.data.model.YogaRoutine
import com.example.ui.viewmodel.AiGenerationState
import com.example.watch.WatchH9State

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizeScreen(
    profile: UserProfile?,
    watchState: WatchH9State,
    aiState: AiGenerationState,
    onGenerateRoutine: (
        focusArea: String,
        experienceLevel: String,
        durationMinutes: Int,
        userNote: String,
        useWatchH9Metrics: Boolean,
    ) -> Unit,
    onOpenGeneratedRoutine: (YogaRoutine) -> Unit,
    onStartGeneratedRoutine: (YogaRoutine) -> Unit,
    onResetAiState: () -> Unit,
) {
    var selectedFocus by rememberSaveable {
        mutableStateOf(profile?.primaryGoal ?: BeginnerYogaCatalog.focusAreas.first())
    }
    var selectedLevel by rememberSaveable {
        mutableStateOf(profile?.experienceLevel ?: "brand_new")
    }
    var selectedMinutes by rememberSaveable {
        mutableIntStateOf(profile?.preferredMinutes ?: 15)
    }
    var userNote by rememberSaveable { mutableStateOf("") }
    var useWatchH9Metrics by rememberSaveable { mutableStateOf(true) }

    val activeWatchHr = watchState.liveHeartRateBpm ?: profile?.lastRestingHr

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Personalize Your Flow",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Build a beginner-safe yoga routine tailored to how your body feels today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "1. Focus Area",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BeginnerYogaCatalog.focusAreas.forEach { area ->
                            val selected = selectedFocus == area
                            FilterChip(
                                selected = selected,
                                onClick = { selectedFocus = area },
                                label = { Text(area) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    Text(
                        text = "2. Beginner Comfort Level",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BeginnerYogaCatalog.experienceLevels.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedLevel == key,
                                onClick = { selectedLevel = key },
                                label = { Text(label) },
                            )
                        }
                    }

                    Text(
                        text = "3. Practice Length",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                label = { Text("${mins}m") },
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Watch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column {
                                    Text(
                                        text = "Adapt using Watch H9 HR",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = if (activeWatchHr != null) {
                                            "Using latest Watch H9 reading: $activeWatchHr BPM"
                                        } else {
                                            "No Watch H9 reading synced yet"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = useWatchH9Metrics,
                                onCheckedChange = { useWatchH9Metrics = it },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = userNote,
                        onValueChange = { if (it.length <= 180) userNote = it },
                        label = { Text("Anything tight or sensitive today? (Optional)") },
                        placeholder = { Text("e.g., stiff lower back, sensitive wrists") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalize_note_input"),
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                    )

                    Button(
                        onClick = {
                            onGenerateRoutine(
                                selectedFocus,
                                selectedLevel,
                                selectedMinutes,
                                userNote,
                                useWatchH9Metrics,
                            )
                        },
                        enabled = aiState !is AiGenerationState.Generating,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_routine_button"),
                    ) {
                        if (aiState is AiGenerationState.Generating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Crafting your blue-calm flow...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Personalized Routine")
                        }
                    }
                }
            }
        }

        when (aiState) {
            is AiGenerationState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Personalization Notice",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            OutlinedButton(onClick = onResetAiState) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            is AiGenerationState.Ready -> {
                val routine = aiState.routine
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Ready & Saved to Your Library",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = routine.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = routine.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                            Text(
                                text = "${routine.poses.size} beginner poses · Watch H9 Target ${routine.targetHrMin}-${routine.targetHrMax} BPM",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenGeneratedRoutine(routine) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("View Poses")
                                }
                                Button(
                                    onClick = { onStartGeneratedRoutine(routine) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Practice Now")
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
