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
fun OpenAacApp() {
    val context = LocalContext.current
    var setupComplete by remember { mutableStateOf(Store.isSetup(context)) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var voiceOptions by remember { mutableStateOf(emptyList<VoiceOption>()) }
    var selectedVoiceName by remember { mutableStateOf(Store.voiceName(context)) }
    var speechRate by remember { mutableStateOf(Store.speechRate(context)) }
    var currentProfile by remember { mutableStateOf(Store.currentProfile(context)) }
    var profileList by remember { mutableStateOf(Store.profiles(context)) }
    var sentenceSpeaking by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith("sentence_") == true) {
                    mainHandler.post { sentenceSpeaking = true }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("sentence_") == true) {
                    mainHandler.post { sentenceSpeaking = false }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith("sentence_") == true) {
                    mainHandler.post { sentenceSpeaking = false }
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId?.startsWith("sentence_") == true) {
                    mainHandler.post { sentenceSpeaking = false }
                }
            }
        })
        tts = engine
        onDispose { engine.shutdown() }
    }

    LaunchedEffect(ttsReady, selectedVoiceName, speechRate) {
        val engine = tts ?: return@LaunchedEffect
        if (!ttsReady) return@LaunchedEffect
        val localEnglishVoices = engine.voices
            ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
            ?.sortedWith(
                compareByDescending<Voice> { it.locale.country == "AU" }
                    .thenByDescending { it.quality }
                    .thenBy { it.locale.displayName }
                    .thenBy { it.name },
            )
            .orEmpty()
        val localeCounts = mutableMapOf<String, Int>()
        voiceOptions = localEnglishVoices.map { voice ->
            val quality = when (voice.quality) {
                Voice.QUALITY_VERY_HIGH -> "very high quality"
                Voice.QUALITY_HIGH -> "high quality"
                Voice.QUALITY_NORMAL -> "standard quality"
                Voice.QUALITY_LOW -> "low quality"
                Voice.QUALITY_VERY_LOW -> "very low quality"
                else -> "quality ${voice.quality}"
            }
            val localeLabel = voice.locale.displayName.ifBlank { "English" }
            val localeNumber = (localeCounts[localeLabel] ?: 0) + 1
            localeCounts[localeLabel] = localeNumber
            VoiceOption(
                name = voice.name,
                label = "$localeLabel $localeNumber",
                detail = "$quality, ${voice.name}",
            )
        }
        val targetVoice = localEnglishVoices.firstOrNull { it.name == selectedVoiceName }
            ?: localEnglishVoices.firstOrNull { it.locale.country == "AU" }
            ?: localEnglishVoices.firstOrNull()
        if (targetVoice != null) {
            engine.voice = targetVoice
            if (selectedVoiceName != targetVoice.name) {
                selectedVoiceName = targetVoice.name
                Store.saveVoiceName(context, targetVoice.name)
            }
        } else {
            engine.language = Locale.ENGLISH
        }
        engine.setSpeechRate(speechRate)
    }

    val speakWord: (String) -> Unit = { text ->
        if (ttsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
        }
    }
    val speakSentence: (String) -> Unit = { text ->
        if (ttsReady && text.isNotBlank()) {
            sentenceSpeaking = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sentence_${System.currentTimeMillis()}")
        }
    }
    val stopSpeech: () -> Unit = {
        tts?.stop()
        sentenceSpeaking = false
    }

    if (!setupComplete) {
        SetupScreen(onComplete = {
            Store.setup(context, it)
            setupComplete = true
        })
    } else {
        CommunicatorScreen(
            speakWord = speakWord,
            speakSentence = speakSentence,
            stopSpeech = stopSpeech,
            sentenceSpeaking = sentenceSpeaking,
            currentProfile = currentProfile,
            profiles = profileList,
            onProfileSelected = { profileId ->
                Store.saveCurrentProfile(context, profileId)
                currentProfile = Store.currentProfile(context)
                profileList = Store.profiles(context)
                selectedVoiceName = Store.voiceName(context)
                speechRate = Store.speechRate(context)
            },
            onAddProfile = { name ->
                val profile = Store.addProfile(context, name)
                currentProfile = profile
                profileList = Store.profiles(context)
                selectedVoiceName = Store.voiceName(context)
                speechRate = Store.speechRate(context)
            },
            voiceOptions = voiceOptions,
            selectedVoiceName = selectedVoiceName,
            speechRate = speechRate,
            onVoiceSelected = {
                selectedVoiceName = it
                Store.saveVoiceName(context, it)
            },
            onSpeechRateChanged = {
                speechRate = it.coerceIn(0.6f, 1.4f)
                Store.saveSpeechRate(context, speechRate)
            },
        )
    }
}

@Composable
fun SetupScreen(onComplete: (String) -> Unit) {
    var passcode by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = passcode.length >= 4 && passcode != "1234" && passcode == confirm

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("OpenAAC setup", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Everything stays on this tablet. No accounts, no internet permission, no cloud database.",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Recommendations start with simple paths. They usually become useful after a few repeated phrases, and noticeably better after a week or two of regular use.",
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF56616F),
        )
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Create an admin passcode", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("The temporary default is 1234, but setup requires a new code.", fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(passcode, { passcode = it.filter(Char::isDigit).take(8) }, label = { Text("New passcode") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(8) }, label = { Text("Confirm passcode") })
                Spacer(Modifier.height(16.dp))
                Button(enabled = valid, onClick = { onComplete(passcode) }) {
                    Text("Start OpenAAC")
                }
            }
        }
    }
}
