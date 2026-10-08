package com.pratheen.arise.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratheen.arise.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/* ============================== HUNTER (home) ============================== */

@Composable
fun HunterScreen(state: HunterState, onOpenQuests: () -> Unit) {
    val rank = state.rankInfo
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CharacterStage(Modifier.fillMaxSize())

            Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
                Eyebrow("HUNTER RANK")
                Text(rank.rank, color = Ink.TextBright, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(rank.title, color = Ink.TextDim, fontSize = 13.sp)
            }
            Column(Modifier.align(Alignment.TopEnd).padding(20.dp), horizontalAlignment = Alignment.End) {
                Eyebrow("LEVEL")
                Text(state.level.toString().padStart(2, '0'), color = Ink.TextBright, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("${state.xp} TOTAL XP", color = Ink.TextDim, fontSize = 12.sp)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(
                    "PLAYER \u00B7 ${if (state.rested) "RECOVERING" else "AWAKENED"}",
                    color = Ink.Mint, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (state.level == 100) "Shadow Monarch" else "Shadow in the making",
                    color = Ink.TextBright, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Ink.Panel, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    if (state.level == 100) "MAXIMUM RANK" else "NEXT AWAKENING",
                    color = Ink.TextDim, fontSize = 12.sp, letterSpacing = 1.sp
                )
                Text(
                    if (state.level == 100) "S RANK" else "${state.xp % 200} / 200 XP",
                    color = Ink.TextBright, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            ThinProgressBar(state.xpFraction)
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                QuickStat(Icons.Filled.DirectionsWalk, "%,d".format(state.steps), "STEPS TODAY")
                QuickStat(Icons.Filled.Explore, "%.2f km".format(state.km), "DISTANCE")
                QuickStat(Icons.Filled.MilitaryTech, "${state.earned.count { it }} / 5", "QUESTS CLEARED")
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Ink.VioletDim, Ink.Violet)))
                    .clickable(onClick = onOpenQuests)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bolt, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("DAILY QUEST \u00B7 +200 XP", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("Open today's training", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String) = Text(text, color = Ink.TextDim, fontSize = 10.sp, letterSpacing = 2.sp)

@Composable
private fun QuickStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Ink.Mint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Ink.TextBright, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Ink.TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ThinProgressBar(fraction: Float) {
    val animated by animateFloatAsState(min(1f, fraction), label = "xp")
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.4f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).background(Brush.horizontalGradient(listOf(Ink.Violet, Ink.Mint))))
    }
}

/* ============================== QUESTS ============================== */

@Composable
fun QuestsScreen(
    state: HunterState,
    stepsConnected: Boolean,
    onTrackReps: (Int) -> Unit,
    gpsRunning: Boolean,
    liveExtraKm: Double = 0.0,
    onToggleGps: () -> Unit,
    onManageSources: () -> Unit,
    onToggleRest: () -> Unit
) {
    val displayKm = state.km + liveExtraKm
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", Locale.US)) }
    val clearedCount = state.earned.count { it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Eyebrow("DAILY QUEST / $today")
        Text("Become stronger.", color = Ink.TextBright, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Move in the real world. Level up here.", color = Ink.TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ink.Panel).padding(16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            LabeledStat("COMPLETION", "$clearedCount / 5")
            Box(
                Modifier.size(56.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Ink.Violet, Ink.Mint, Ink.Violet))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Bolt, null, tint = Color.White) }
            LabeledStat("REWARD", "${clearedCount * 40} XP", alignEnd = true)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, null, tint = Ink.TextDim, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Quests complete and XP is awarded automatically when activity reaches its target.",
                color = Ink.TextDim, fontSize = 12.sp, lineHeight = 16.sp
            )
        }
        Spacer(Modifier.height(8.dp))

        EXERCISE_NAMES.forEachIndexed { i, name ->
            QuestRow(
                name = name,
                cleared = state.earned[i],
                value = state.reps[i],
                target = state.target.reps,
                sourceLabel = if (i == 0) "PHONE PROXIMITY" else "PHONE SENSOR / WATCH REPS",
                enabled = !state.rested && !state.earned[i],
                onTrack = { onTrackReps(i) }
            )
        }

        ActivityCard(
            title = "Walk / run", icon = Icons.Filled.Explore,
            cleared = state.earned[3],
            bigValue = "%.2f".format(displayKm), bigTarget = "/ %.2f km".format(state.target.km),
            progress = (displayKm / state.target.km).toFloat(),
            note = "Synced walking/running sessions also count. Cycling distance is excluded."
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (gpsRunning) Ink.Danger else Ink.Violet)
                    .clickable(enabled = !state.rested, onClick = onToggleGps)
                    .padding(14.dp),
                Arrangement.Center, Alignment.CenterVertically
            ) {
                Icon(if (gpsRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (gpsRunning) "Finish GPS journey" else "Start GPS journey", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        ActivityCard(
            title = "Daily steps", icon = Icons.Filled.DirectionsWalk,
            cleared = state.earned[4],
            bigValue = "%,d".format(state.steps), bigTarget = "/ %,d".format(state.target.steps),
            progress = (state.steps.toFloat() / state.target.steps),
            note = if (stepsConnected) "Automatic steps while Arise is open." else "Connect your phone sensor to count steps."
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Ink.VioletDim, RoundedCornerShape(14.dp))
                    .clickable(onClick = onManageSources)
                    .padding(14.dp),
                Arrangement.Center, Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Link, null, tint = Ink.Violet, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Manage activity sources", color = Ink.Violet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(Ink.Panel).padding(16.dp)
        ) {
            Icon(Icons.Filled.Favorite, null, tint = Ink.Mint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Recovery is part of the quest.", color = Ink.TextBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Take breaks. Stop if movement hurts. No penalties for missed days.", color = Ink.TextDim, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.rested) "Resume training" else "Take a recovery day",
                    color = Ink.Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onToggleRest)
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun LabeledStat(label: String, value: String, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, color = Ink.TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = Ink.TextBright, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuestRow(name: String, cleared: Boolean, value: Int, target: Int, sourceLabel: String, enabled: Boolean, onTrack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (cleared) Ink.Plum else Ink.Panel)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(if (cleared) Ink.Mint else Ink.VioletDim),
                contentAlignment = Alignment.Center
            ) { Icon(if (cleared) Icons.Filled.Check else Icons.Filled.Bolt, null, tint = if (cleared) Ink.Void else Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink.TextBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(sourceLabel, color = Ink.TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Row {
                Text("$value", color = Ink.TextBright, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(" / $target", color = Ink.TextDim, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        ThinProgressBar(value.toFloat() / target)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(
                if (cleared) "QUEST CLEARED \u00B7 40 XP" else "REWARD \u00B7 +40 XP",
                color = if (cleared) Ink.Mint else Ink.TextDim, fontSize = 11.sp, letterSpacing = 0.5.sp
            )
            Text(
                if (cleared) "Completed" else "Track reps",
                color = if (enabled) Ink.Violet else Ink.TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = enabled, onClick = onTrack)
            )
        }
    }
}

@Composable
private fun ActivityCard(
    title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, cleared: Boolean,
    bigValue: String, bigTarget: String, progress: Float, note: String, action: @Composable () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp))
            .background(if (cleared) Ink.Plum else Ink.Panel).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Ink.Mint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Ink.TextBright, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (cleared) "\u2713 CLEARED" else "+40 XP", color = if (cleared) Ink.Mint else Ink.TextDim, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(bigValue, color = Ink.TextBright, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(" $bigTarget", color = Ink.TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        ThinProgressBar(progress)
        Spacer(Modifier.height(10.dp))
        Text(note, color = Ink.TextDim, fontSize = 11.5.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(10.dp))
        action()
    }
}

/* ============================== CONNECT ============================== */

@Composable
fun ConnectScreen(
    stepsConnected: Boolean, onConnectPhone: () -> Unit,
    healthConnected: Boolean, onConnectHealth: () -> Unit, onSyncHealth: () -> Unit,
    sources: List<String>, syncedAt: Long, onPrivacy: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Eyebrow("SYSTEM LINK")
        Text("Your world. Connected.", color = Ink.TextBright, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Your activity becomes your progress.", color = Ink.TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        ConnectionCard(
            icon = Icons.Filled.Smartphone, title = "Phone sensors",
            body = if (stepsConnected) "Phone step sensor connected" else "Phone sensor not connected",
            note = "Automatic steps while Arise is open. Reconnects after permission is granted."
        ) {
            PrimaryPill(if (stepsConnected) "Reconnect phone" else "Connect phone", Icons.Filled.Sensors, onConnectPhone)
        }

        ConnectionCard(
            icon = Icons.Filled.Watch, title = "Watch & health apps",
            body = if (healthConnected) "Health Connect linked" else "Connect Health Connect to sync your watch",
            note = "Android 14+. In your watch's companion app, enable sharing to Health Connect. Then allow Arise to read steps and distance."
        ) {
            Column {
                PrimaryPill(if (healthConnected) "Manage Health Connect" else "Connect Health Connect", Icons.Filled.Favorite, onConnectHealth)
                Spacer(Modifier.height(8.dp))
                SecondaryPill("Sync now", Icons.Filled.Sync, onSyncHealth)
                if (sources.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("RECEIVING DATA FROM", color = Ink.TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                    sources.forEach { Text(it, color = Ink.TextBright, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
                if (syncedAt > 0) {
                    Spacer(Modifier.height(6.dp))
                    val time = java.text.SimpleDateFormat("h:mm a", Locale.US).format(java.util.Date(syncedAt))
                    Text("Last read: $time", color = Ink.TextDim, fontSize = 11.sp)
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(Ink.Panel).padding(16.dp)) {
            Text("One connection. Compatible watches.", color = Ink.TextBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Watch support depends on its companion app sharing data to Health Connect. A Bluetooth pairing alone does not provide activity records. Arise uses the larger of the phone or synced total, rather than adding both.",
                color = Ink.TextDim, fontSize = 12.sp, lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clickable(onClick = onPrivacy).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Shield, null, tint = Ink.TextDim, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Data & privacy", color = Ink.TextDim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ConnectionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, note: String, action: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(Ink.Panel).padding(18.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Ink.VioletDim), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = Ink.TextBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Ink.Mint, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        action()
        Spacer(Modifier.height(8.dp))
        Text(note, color = Ink.TextDim, fontSize = 11.5.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun PrimaryPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Ink.VioletDim, Ink.Violet)))
            .clickable(onClick = onClick).padding(14.dp),
        Arrangement.Center, Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun SecondaryPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, Ink.VioletDim, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(14.dp),
        Arrangement.Center, Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Ink.Violet, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Ink.Violet, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/* ============================== RANK ============================== */

@Composable
fun RankScreen(state: HunterState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Eyebrow("PLAYER PROGRESSION")
        Text("Rise through the ranks.", color = Ink.TextBright, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("${state.days} training days cleared \u00B7 ${state.cleared} quests", color = Ink.TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        RANKS.forEach { r ->
            val active = r.rank == state.rankInfo.rank
            val unlocked = state.level >= r.level
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (active) Ink.Violet.copy(alpha = 0.18f) else Ink.Panel)
                    .border(if (active) 1.dp else 0.dp, Ink.Violet, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(if (unlocked) Ink.Mint else Ink.Plum),
                    contentAlignment = Alignment.Center
                ) { Text(r.rank, color = if (unlocked) Ink.Void else Ink.TextDim, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("LEVEL ${r.level}", color = Ink.TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(r.title, color = Ink.TextBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (active) "CURRENT RANK" else if (unlocked) "UNLOCKED" else "LOCKED",
                        color = if (active) Ink.Mint else Ink.TextDim, fontSize = 10.sp, letterSpacing = 1.sp
                    )
                }
                Icon(
                    if (unlocked) Icons.Filled.Check else Icons.Filled.Lock,
                    null, tint = if (unlocked) Ink.Mint else Ink.TextDim, modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ink.Panel).padding(16.dp)) {
            Text("The ultimate daily quest", color = Ink.TextBright, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "100 push-ups. 100 sit-ups. 100 squats. 10 km. Your first day starts at 5 reps, 0.5 km and 1,000 steps; targets grow after completed training days.",
                color = Ink.TextDim, fontSize = 12.sp, lineHeight = 17.sp
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}

/* ============================== NAV / SESSION / TOAST ============================== */

data class Tab(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val TABS = listOf(
    Tab("hunter", "Hunter", Icons.Filled.Person),
    Tab("quests", "Quests", Icons.Filled.Bolt),
    Tab("connect", "Connect", Icons.Filled.Link),
    Tab("rank", "Rank", Icons.Filled.MilitaryTech)
)

@Composable
fun BottomNav(current: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Ink.Void).padding(vertical = 8.dp)) {
        TABS.forEach { tab ->
            val selected = tab.key == current
            Column(
                Modifier.weight(1f).clickable { onSelect(tab.key) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(tab.icon, null, tint = if (selected) Ink.Violet else Ink.TextDim, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp))
                Text(tab.label, color = if (selected) Ink.Violet else Ink.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SessionPanel(exercise: Int, message: String, count: Int, onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(24.dp)).background(Ink.Panel).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Eyebrow("PHONE SENSOR TRACKING")
            Spacer(Modifier.height(8.dp))
            Text(EXERCISE_NAMES[exercise], color = Ink.TextBright, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Text("$count", color = Ink.Violet, fontSize = 56.sp, fontWeight = FontWeight.Black)
            Text("REPS THIS SESSION", color = Ink.TextDim, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(14.dp))
            Text(message, color = Ink.TextDim, fontSize = 13.sp, lineHeight = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink.Violet)
                    .clickable(onClick = onFinish).padding(14.dp),
                Arrangement.Center
            ) { Text("Finish session", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ToastBanner(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) { kotlinx.coroutines.delay(2600); onDismiss() }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.padding(bottom = 90.dp).clip(RoundedCornerShape(20.dp)).background(Ink.Panel).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = Ink.TextBright, fontSize = 13.sp)
        }
    }
}
