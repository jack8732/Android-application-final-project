@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.fitpick.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fitpick.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ============================ SHARED COMPONENTS ============================
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val shape = RoundedCornerShape(20.dp)
    val elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
fun InfoChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun EmojiBadge(emoji: String) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 24.sp) }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)

private fun emojiFor(muscles: String): String =
    Muscle.values().firstOrNull { muscles.contains(it.label) }?.emoji ?: "💪"

// ============================ HOME ============================
@Composable
fun HomeScreen(vm: AppViewModel, onCreate: () -> Unit, onOpen: () -> Unit) {
    val routines by vm.routines.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Orange, Pink, Purple)))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🏋️ FitPick", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Pick your muscles. Get an AI workout in seconds.", color = Color.White.copy(alpha = 0.92f))
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onCreate,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFE8531F)),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New AI routine", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${logs.size}", "Workouts done", Modifier.weight(1f))
                StatTile("${routines.size}", "Saved routines", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Saved routines") }
        if (routines.isEmpty()) {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("Nothing saved yet. Create a routine and tap Save.", Modifier.padding(16.dp))
                }
            }
        }
        items(routines, key = { it.id }) { r ->
            AppCard(Modifier.fillMaxWidth(), onClick = { vm.openSaved(r); onOpen() }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emojiFor(r.muscles))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${r.muscles} - ${r.minutes} min - ${r.level}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { vm.delete(r) }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

// ============================ CREATE ============================
@Composable
fun MuscleTile(m: Muscle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val bg = if (selected) Modifier.background(Brush.linearGradient(listOf(Orange, Pink)))
    else Modifier.background(MaterialTheme.colorScheme.surface)
    Column(
        modifier
            .height(88.dp)
            .clip(shape)
            .then(bg)
            .border(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(m.emoji, fontSize = 28.sp)
        Text(
            m.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun <T> OptionRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = { Text(label(o)) },
                colors = chipColors()
            )
        }
    }
}

@Composable
fun CreateScreen(vm: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val loading = vm.state is GenState.Loading
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("New routine", onBack)
        SectionTitle("Which muscles do you want to train?")
        Muscle.values().toList().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    MuscleTile(m, m in vm.selected, Modifier.weight(1f)) { vm.toggle(m) }
                }
            }
        }
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionRow("Level", LEVELS, vm.level, { it }) { vm.level = it }
                OptionRow("Equipment", EQUIPMENT, vm.equipment, { it }) { vm.equipment = it }
                OptionRow("Goal", GOALS, vm.goal, { it }) { vm.goal = it }
                OptionRow("Time", DURATIONS, vm.minutes, { "$it min" }) { vm.minutes = it }
            }
        }
        (vm.state as? GenState.Error)?.let {
            Text(it.msg, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { vm.generate(onDone) },
            enabled = vm.selected.isNotEmpty() && !loading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(10.dp))
                Text("Building your workout...", fontWeight = FontWeight.Bold)
            } else {
                Text(
                    if (vm.selected.isEmpty()) "Pick at least one muscle" else "Generate with AI",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================ ROUTINE ============================
@Composable
fun RoutineScreen(vm: AppViewModel, onBack: () -> Unit, onStart: () -> Unit) {
    val r = vm.current
    if (r == null) {
        Text("No routine selected", Modifier.padding(16.dp))
        return
    }
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenHeader(r.title, onBack)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(r.muscles)
                    InfoChip("${r.minutes} min")
                    InfoChip(r.level)
                    InfoChip(r.goal)
                }
                Text(r.source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    "AI swaps left: ${MAX_SWAPS - vm.swapsUsed} / $MAX_SWAPS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        itemsIndexed(r.exercises) { i, e ->
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Orange, Pink))),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            e.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (vm.swapping == i) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            val canSwap = vm.swapsUsed < MAX_SWAPS
                            IconButton(onClick = { vm.swap(i) }, enabled = canSwap) {
                                Icon(
                                    Icons.Filled.Refresh, "Swap exercise",
                                    tint = if (canSwap) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    if (e.imageUrl != null) {
                        AsyncImage(
                            model = e.imageUrl,
                            contentDescription = e.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                                .clip(RoundedCornerShape(14.dp)).background(Color.White)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip("${e.sets} sets")
                        InfoChip("${e.reps} reps")
                        InfoChip("${e.restSec}s rest")
                    }
                    if (e.tip.isNotBlank()) {
                        Text("💡 ${e.tip}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start workout", fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { vm.save() },
                        enabled = !vm.currentSaved,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (vm.currentSaved) "Saved" else "Save") }
                    OutlinedButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, vm.shareText(r))
                            }
                            context.startActivity(Intent.createChooser(send, "Share routine"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }
                Text(
                    "Not medical advice. Stop if something hurts and ask a professional if you have an injury.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================ WORKOUT ============================
@Composable
fun WorkoutScreen(vm: AppViewModel, onBack: () -> Unit, onHome: () -> Unit, onProgress: () -> Unit) {
    val r = vm.current
    if (r == null) {
        Text("No routine selected", Modifier.padding(16.dp))
        return
    }
    val done = remember { mutableStateListOf<Int>().apply { repeat(r.exercises.size) { add(0) } } }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var rest by remember { mutableIntStateOf(0) }
    var restKey by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(restKey) {
        while (rest > 0) {
            delay(1000)
            rest -= 1
        }
    }

    val finishedText = summary
    if (finishedText != null) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Teal, Purple)))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎉 Workout complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(finishedText, color = Color.White.copy(alpha = 0.95f))
                }
            }
            Button(
                onClick = {
                    for (i in done.indices) done[i] = 0
                    rest = 0
                    startedAt = System.currentTimeMillis()
                    summary = null
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Restart this workout", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Back to routine") }
            OutlinedButton(onClick = onHome, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Home") }
            OutlinedButton(onClick = onProgress, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("View progress") }
        }
        return
    }

    val totalSets = r.exercises.sumOf { it.sets }
    val doneSets = r.exercises.indices.sumOf { minOf(done[it], r.exercises[it].sets) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScreenHeader(r.title, onBack)
                Text("$doneSets / $totalSets sets", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { if (totalSets == 0) 0f else doneSets / totalSets.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        if (rest > 0) {
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Teal, Purple)))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Rest", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                            Text("${rest}s", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        TextButton(onClick = { rest = 0 }) { Text("Skip", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        itemsIndexed(r.exercises) { i, e ->
            val complete = done[i] >= e.sets
            AppCard(
                Modifier.fillMaxWidth(),
                containerColor = if (complete) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${e.reps} reps", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(e.sets) { s ->
                                Box(
                                    Modifier.size(14.dp).clip(CircleShape).background(
                                        if (s < done[i]) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }
                    }
                    if (complete) {
                        Icon(Icons.Filled.CheckCircle, "Done", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                    } else {
                        Button(
                            onClick = {
                                done[i] = done[i] + 1
                                rest = e.restSec
                                restKey += 1
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Set done", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    val completed = r.exercises.indices.count { done[it] >= r.exercises[it].sets }
                    val secs = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                    vm.finishWorkout(completed, r.exercises.size, secs)
                    rest = 0
                    summary = "$completed of ${r.exercises.size} exercises done in ${maxOf(1, secs / 60)} min. Saved to your progress."
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Finish workout", fontWeight = FontWeight.Bold) }
        }
    }
}

// ============================ HISTORY / PROGRESS ============================
@Composable
fun HistoryScreen(vm: AppViewModel) {
    val logs by vm.logs.collectAsStateWithLifecycle()
    val goal by vm.weeklyGoal.collectAsStateWithLifecycle()

    val dayMs = 86_400_000L
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val days = (6 downTo 0).map { today - it * dayMs }
    val counts = days.map { s -> logs.count { it.completedAt >= s && it.completedAt < s + dayMs } }
    val weekTotal = counts.sum()
    val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val logFmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$weekTotal", "This week", Modifier.weight(1f))
                StatTile("${logs.size}", "All workouts", Modifier.weight(1f))
                StatTile("${logs.sumOf { it.durationSec } / 60}", "Minutes", Modifier.weight(1f))
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Weekly goal: $weekTotal / $goal", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (weekTotal / goal.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(Modifier.fillMaxWidth().height(130.dp), verticalAlignment = Alignment.Bottom) {
                        days.forEachIndexed { i, d ->
                            val barBrush: Brush = if (counts[i] > 0) Brush.verticalGradient(listOf(Orange, Pink))
                            else SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Column(
                                Modifier.weight(1f).fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (counts[i] > 0) Text("${counts[i]}", style = MaterialTheme.typography.labelSmall)
                                Box(
                                    Modifier.width(22.dp).height(maxOf(6, counts[i] * 28).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(barBrush)
                                )
                                Text(dayFmt.format(Date(d)), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("History") }
        if (logs.isEmpty()) {
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("No workouts yet. Finish a workout to see it here.", Modifier.padding(16.dp))
                }
            }
        }
        items(logs, key = { it.id }) { l ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emojiFor(l.muscles))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(l.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${l.completedExercises}/${l.totalExercises} exercises - ${l.durationSec / 60} min", style = MaterialTheme.typography.bodySmall)
                        Text(logFmt.format(Date(l.completedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ============================ SETTINGS ============================
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val savedKey by vm.apiKey.collectAsStateWithLifecycle()
    val savedGoal by vm.weeklyGoal.collectAsStateWithLifecycle()
    val savedModel by vm.model.collectAsStateWithLifecycle()
    var key by remember(savedKey) { mutableStateOf(savedKey) }
    var modelName by remember(savedModel) { mutableStateOf(savedModel) }
    var goal by remember(savedGoal) { mutableIntStateOf(savedGoal) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("🤖 AI")
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Gemini API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Gemini model") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Get a free key at aistudio.google.com. If you see a 404 error, enter a current model name from ai.google.dev/gemini-api/docs/models. Without a key the app builds a basic offline routine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("🎯 Weekly workout goal: $goal")
                Slider(
                    value = goal.toFloat(),
                    onValueChange = { goal = it.roundToInt() },
                    valueRange = 1f..7f,
                    steps = 5
                )
            }
        }
        Button(
            onClick = { vm.saveSettings(key, goal, modelName) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("Save settings", fontWeight = FontWeight.Bold) }
        OutlinedButton(
            onClick = { vm.clearHistory() },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Clear workout history") }
    }
}
