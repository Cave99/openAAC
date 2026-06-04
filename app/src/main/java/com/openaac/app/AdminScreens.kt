package com.openaac.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale


@Composable
fun AdminLogin(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Admin", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(passcode, { passcode = it.filter(Char::isDigit).take(8) }, label = { Text("Passcode") })
                if (error) Text("Wrong passcode", color = Color(0xFFD53B3B))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("cancel") }
                    Button(onClick = {
                        if (passcode == Store.passcode(context)) {
                            keyboard?.hide()
                            onSuccess()
                        } else {
                            error = true
                        }
                    }) { Text("open") }
                }
            }
        }
    }
}

@Composable
fun AdminScreen(
    onRestoreLayout: () -> Unit,
    onRestoreAll: () -> Unit,
    onWipeLearning: () -> Unit,
    currentProfile: ChildProfile,
    profiles: List<ChildProfile>,
    onProfileSelected: (String) -> Unit,
    onAddProfile: (String) -> Unit,
    voiceOptions: List<VoiceOption>,
    selectedVoiceName: String?,
    speechRate: Float,
    onVoiceSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onTestVoice: () -> Unit,
    usageRefresh: Int,
    onEnterReorganize: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var insights by remember { mutableStateOf(Store.usageInsights(context)) }
    var showAddProfile by remember { mutableStateOf(false) }
    var showPasscodeReset by remember { mutableStateOf(false) }
    LaunchedEffect(usageRefresh, currentProfile.id) {
        insights = Store.usageInsights(context)
    }

    FullScreenOverlay {
        Card(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.92f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Admin editor", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = onEnterReorganize) { Text("reorganize board") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onRestoreLayout) { Text("restore layout") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onRestoreAll) { Text("restore all") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onClose) { Text("done") }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier
                            .weight(1.45f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileSelector(
                            currentProfile = currentProfile,
                            profiles = profiles,
                            onProfileSelected = onProfileSelected,
                            onAddProfile = { showAddProfile = true },
                        )
                        Text("Board editing", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Use reorganize board to return to the communication screen, keep folders navigable, drag words into place, add words, and save with Done.",
                            fontSize = 17.sp,
                            color = Color(0xFF56616F),
                        )
                        Button(onClick = onEnterReorganize, modifier = Modifier.height(58.dp)) {
                            Text("start reorganizing")
                        }
                        OutlinedButton(onClick = { showPasscodeReset = true }) { Text("reset passcode") }
                        OutlinedButton(onClick = onWipeLearning) { Text("wipe learned history") }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VoiceControls(
                            currentProfile = currentProfile,
                            voiceOptions = voiceOptions,
                            selectedVoiceName = selectedVoiceName,
                            speechRate = speechRate,
                            onVoiceSelected = onVoiceSelected,
                            onSpeechRateChanged = onSpeechRateChanged,
                            onTestVoice = onTestVoice,
                        )
                        UsageDashboard(insights)
                    }
                }
            }
        }
    }

    if (showAddProfile) {
        AddProfileDialog(
            onDismiss = { showAddProfile = false },
            onAdd = {
                onAddProfile(it)
                showAddProfile = false
            },
        )
    }

    if (showPasscodeReset) {
        PasscodeResetDialog(
            onDismiss = { showPasscodeReset = false },
            onSaved = { showPasscodeReset = false },
        )
    }
}

@Composable
fun ProfileSelector(
    currentProfile: ChildProfile,
    profiles: List<ChildProfile>,
    onProfileSelected: (String) -> Unit,
    onAddProfile: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7F1))) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Profile", fontSize = 14.sp, color = Color(0xFF56616F))
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(currentProfile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = {
                                    expanded = false
                                    onProfileSelected(profile.id)
                                },
                            )
                        }
                    }
                }
            }
            Button(onClick = onAddProfile) { Text("add profile") }
        }
    }
}

@Composable
fun UsageDashboard(insights: UsageInsights) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F6FA))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Usage this month", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${insights.uniqueWordsThisMonth}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2166F3))
                    Text("unique words", fontSize = 13.sp, color = Color(0xFF56616F))
                }
                val trendText = when {
                    insights.uniqueTrend > 0 -> "up ${insights.uniqueTrend} from last month"
                    insights.uniqueTrend < 0 -> "down ${kotlin.math.abs(insights.uniqueTrend)} from last month"
                    else -> "same as last month"
                }
                Text(trendText, fontSize = 15.sp, color = Color(0xFF56616F), modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
            InsightList("Top words", insights.topWords, emptyText = "No words tracked yet.")
            HorizontalDivider()
            InsightList("Top sentences", insights.topSentences, emptyText = "No spoken sentences yet.")
        }
    }
}

@Composable
fun InsightList(title: String, rows: List<Pair<String, Int>>, emptyText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (rows.isEmpty()) {
            Text(emptyText, fontSize = 14.sp, color = Color(0xFF56616F))
        } else {
            rows.forEachIndexed { index, (label, count) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}", fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF56616F)).padding(vertical = 2.dp))
                    Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$count", fontSize = 14.sp, color = Color(0xFF56616F))
                }
            }
        }
    }
}

@Composable
fun AddProfileDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier
                    .imePadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Add profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Child name") })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        keyboard?.hide()
                        onDismiss()
                    }) { Text("cancel") }
                    Button(enabled = name.isNotBlank(), onClick = {
                        keyboard?.hide()
                        onAdd(name)
                    }) { Text("create") }
                }
            }
        }
    }
}

@Composable
fun PasscodeResetDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var oldPasscode by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val canSave = oldPasscode.isNotBlank() && newPasscode.length >= 4 && newPasscode == confirmPasscode

    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Reset passcode", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(oldPasscode, { oldPasscode = it.filter(Char::isDigit).take(8) }, label = { Text("Old passcode") })
                OutlinedTextField(newPasscode, { newPasscode = it.filter(Char::isDigit).take(8) }, label = { Text("New passcode") })
                OutlinedTextField(confirmPasscode, { confirmPasscode = it.filter(Char::isDigit).take(8) }, label = { Text("Confirm new passcode") })
                error?.let { Text(it, color = Color(0xFFD53B3B), fontSize = 14.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        keyboard?.hide()
                        onDismiss()
                    }) { Text("cancel") }
                    Button(
                        enabled = canSave,
                        onClick = {
                            when {
                                oldPasscode != Store.passcode(context) -> error = "Old passcode is wrong."
                                newPasscode == "1234" -> error = "Choose a passcode other than 1234."
                                else -> {
                                    Store.savePasscode(context, newPasscode)
                                    keyboard?.hide()
                                    onSaved()
                                }
                            }
                        },
                    ) { Text("save") }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(label: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Delete button?", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Remove \"$label\" from this board?",
                    fontSize = 17.sp,
                    color = Color(0xFF56616F),
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("cancel") }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05555)),
                    ) { Text("delete") }
                }
            }
        }
    }
}

@Composable
fun VoiceControls(
    currentProfile: ChildProfile,
    voiceOptions: List<VoiceOption>,
    selectedVoiceName: String?,
    speechRate: Float,
    onVoiceSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onTestVoice: () -> Unit,
) {
    val currentIndex = voiceOptions.indexOfFirst { it.name == selectedVoiceName }.takeIf { it >= 0 } ?: 0
    val current = voiceOptions.getOrNull(currentIndex)
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F6FA))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Voice for ${currentProfile.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Saved to this profile", fontSize = 13.sp, color = Color(0xFF56616F))
            Box {
                OutlinedButton(
                    enabled = voiceOptions.isNotEmpty(),
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        current?.label ?: "Default Android English voice",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    voiceOptions.forEach { voice ->
                        DropdownMenuItem(
                            text = { Text(voice.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                expanded = false
                                onVoiceSelected(voice.name)
                            },
                        )
                    }
                }
            }
            current?.detail?.let { detail ->
                Text(detail, fontSize = 12.sp, color = Color(0xFF56616F), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onTestVoice) { Text("test") }
                Text("Speed ${(speechRate * 100).toInt()}%", fontSize = 14.sp, color = Color(0xFF56616F))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSpeechRateChanged(speechRate - 0.1f) }) { Text("slower") }
                    OutlinedButton(onClick = { onSpeechRateChanged(speechRate + 0.1f) }) { Text("faster") }
                }
            }
        }
    }
}
