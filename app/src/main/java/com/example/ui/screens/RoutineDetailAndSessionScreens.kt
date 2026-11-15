package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.catalog.BeginnerYogaCatalog
import com.example.data.model.YogaRoutine
import com.example.ui.theme.AzureGlow
import com.example.ui.theme.CobaltDeep
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SkyMist
import com.example.watch.WatchConnectionStatus
import com.example.watch.WatchH9State
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routine: YogaRoutine,
    watchState: WatchH9State,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    onPushToWatchH9: () -> Unit,
    onDeleteRoutine: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routine Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (!routine.id.startsWith("curated_") && routine.id.isNotBlank()) {
                        IconButton(onClick = onDeleteRoutine) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete saved routine",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CobaltDeep),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${routine.focusArea} · ${BeginnerYogaCatalog.levelLabel(routine.level)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyMist,
                        )
                        Text(
                            text = routine.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                        Text(
                            text = routine.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                        Text(
                            text = "${routine.durationMinutes} min · ${routine.poses.size} poses · Watch H9 Target ${routine.targetHrMin}-${routine.targetHrMax} BPM",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onStartPractice,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = CobaltDeep,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("detail_start_practice_button"),
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Practice")
                            }
                            FilledTonalButton(
                                onClick = onPushToWatchH9,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.height(48.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Watch,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (watchState.status == WatchConnectionStatus.CONNECTED) {
                                        "Sync H9"
                                    } else {
                                        "H9 Cue"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Pose Sequence & Beginner Modifications",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            itemsIndexed(routine.poses) { index, pose ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = pose.name,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    if (pose.sanskritName.isNotBlank()) {
                                        Text(
                                            text = pose.sanskritName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${pose.durationSeconds}s",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Text(
                            text = pose.instruction,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Gentle Modification: ${pose.modification}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Breath: ${pose.breathCue}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun GuidedSessionScreen(
    routine: YogaRoutine,
    watchState: WatchH9State,
    onExit: () -> Unit,
    onCompleteAndLog: (
        actualMinutes: Int,
        posesCompleted: Int,
        avgHr: Int?,
        peakHr: Int?,
        syncedFromWatch: Boolean,
        feeling: String,
    ) -> Unit,
) {
    val poses = routine.poses
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentPose = poses.getOrNull(currentIndex)
    var remainingSeconds by remember(currentIndex) {
        mutableIntStateOf(currentPose?.durationSeconds ?: 45)
    }
    var elapsedTotalSeconds by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var feeling by remember { mutableStateOf("just_right") }

    val hrSamples = remember { mutableStateListOf<Int>() }
    val liveHr = watchState.liveHeartRateBpm

    LaunchedEffect(liveHr, isPlaying, isCompleted) {
        if (liveHr != null && isPlaying && !isCompleted) {
            hrSamples.add(liveHr)
        }
    }

    LaunchedEffect(isPlaying, currentIndex, isCompleted) {
        if (!isPlaying || isCompleted || currentPose == null) return@LaunchedEffect
        while (remainingSeconds > 0 && isPlaying && !isCompleted) {
            delay(1000L)
            remainingSeconds -= 1
            elapsedTotalSeconds += 1
        }
        if (remainingSeconds <= 0 && isPlaying && !isCompleted) {
            if (currentIndex + 1 < poses.size) {
                currentIndex += 1
            } else {
                isCompleted = true
                isPlaying = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breath_halo")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath_pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CobaltDeep, AzureGlow, Color(0xFF0A2E5C))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        if (isCompleted) {
            val avgHr = if (hrSamples.isNotEmpty()) hrSamples.average().toInt() else liveHr
            val peakHr = hrSamples.maxOrNull() ?: liveHr
            val mins = ((elapsedTotalSeconds / 60).coerceAtLeast(1))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = SkyMist,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Namaste · Flow Complete",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${routine.title} · $mins min · ${poses.size} poses",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )

                if (avgHr != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = "Watch H9 Avg HR: $avgHr BPM · Peak: ${peakHr ?: avgHr} BPM",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "How did this pace feel for your body?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "too_gentle" to "Too Gentle",
                        "just_right" to "Just Right",
                        "challenging" to "Challenging",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = feeling == key,
                            onClick = { feeling = key },
                            label = { Text(label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onCompleteAndLog(
                            mins,
                            poses.size,
                            avgHr,
                            peakHr,
                            watchState.status == WatchConnectionStatus.CONNECTED || avgHr != null,
                            feeling,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = CobaltDeep,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_session_save_button"),
                ) {
                    Text("Save & Finish", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else if (currentPose != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Exit session",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "Pose ${currentIndex + 1} of ${poses.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonitorHeart,
                                contentDescription = null,
                                tint = SkyMist,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = liveHr?.let { "$it BPM" } ?: "${routine.targetHrMin}-${routine.targetHrMax}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / poses.size.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = SkyMist,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxRadius = size.minDimension / 2f
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = maxRadius * breathScale,
                            )
                            drawCircle(
                                color = SkyMist.copy(alpha = 0.35f),
                                radius = maxRadius * 0.78f,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = if (isPlaying) "BREATHE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyMist,
                            )
                        }
                    }

                    Text(
                        text = currentPose.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = currentPose.sanskritName,
                        style = MaterialTheme.typography.titleSmall,
                        color = SkyMist,
                    )

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = currentPose.instruction,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Air,
                                    contentDescription = null,
                                    tint = SkyMist,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = currentPose.breathCue,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkyMist,
                                )
                            }
                            Text(
                                text = "Gentle Option: ${currentPose.modification}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { if (currentIndex > 0) currentIndex -= 1 },
                            enabled = currentIndex > 0,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous pose")
                        }

                        Button(
                            onClick = { isPlaying = !isPlaying },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = CobaltDeep,
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("session_play_pause_button"),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Resume",
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (currentIndex + 1 < poses.size) {
                                    currentIndex += 1
                                } else {
                                    isCompleted = true
                                    isPlaying = false
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next pose")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        FilledTonalButton(
                            onClick = { remainingSeconds += 15 },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("+15s extra hold")
                        }
                    }
                }
            }
        }
    }
}
