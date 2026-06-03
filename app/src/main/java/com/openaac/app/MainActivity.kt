package com.openaac.app

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
                    OpenAacApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private data class VocabButton(
    val id: String,
    val label: String,
    val speech: String,
    val icon: String,
    val color: Long,
    val boardId: String? = null,
    val isCategory: Boolean = false,
)

private data class SentenceToken(
    val label: String,
    val speech: String,
    val icon: String,
)

private data class VoiceOption(
    val name: String,
    val label: String,
)

private object Store {
    private const val PREFS = "openaac"
    private const val KEY_SETUP = "setup_complete"
    private const val KEY_PASSCODE = "admin_passcode"
    private const val KEY_HOME = "home_buttons"
    private const val KEY_BOARDS = "board_layouts"
    private const val KEY_SENTENCES = "sentence_history"
    private const val KEY_COUNTS = "usage_counts"
    private const val KEY_TRANSITIONS = "transition_counts"
    private const val KEY_VOICE_NAME = "voice_name"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L

    fun isSetup(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP, false)

    fun setup(context: Context, passcode: String) {
        prefs(context).edit()
            .putBoolean(KEY_SETUP, true)
            .putString(KEY_PASSCODE, passcode)
            .apply()
    }

    fun passcode(context: Context): String =
        prefs(context).getString(KEY_PASSCODE, "1234") ?: "1234"

    fun savePasscode(context: Context, passcode: String) {
        prefs(context).edit().putString(KEY_PASSCODE, passcode).apply()
    }

    fun voiceName(context: Context): String? =
        prefs(context).getString(KEY_VOICE_NAME, null)

    fun saveVoiceName(context: Context, voiceName: String) {
        prefs(context).edit().putString(KEY_VOICE_NAME, voiceName).apply()
    }

    fun speechRate(context: Context): Float =
        prefs(context).getFloat(KEY_SPEECH_RATE, 1.0f)

    fun saveSpeechRate(context: Context, rate: Float) {
        prefs(context).edit().putFloat(KEY_SPEECH_RATE, rate.coerceIn(0.6f, 1.4f)).apply()
    }

    fun homeButtons(context: Context): List<VocabButton> {
        val raw = prefs(context).getString(KEY_HOME, null) ?: return Defaults.home
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toButton() }
        }.getOrDefault(Defaults.home)
    }

    fun saveHomeButtons(context: Context, buttons: List<VocabButton>) {
        val array = JSONArray()
        buttons.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString(KEY_HOME, array.toString()).apply()
    }

    fun boards(context: Context): Map<String, List<VocabButton>> {
        val fallback = Defaults.allBoards()
        val raw = prefs(context).getString(KEY_BOARDS, null)
        if (raw.isNullOrBlank()) {
            val legacyHome = prefs(context).getString(KEY_HOME, null)?.let {
                runCatching {
                    val array = JSONArray(it)
                    List(array.length()) { index -> array.getJSONObject(index).toButton() }
                }.getOrNull()
            }
            return if (legacyHome == null) fallback else fallback + (Defaults.HOME_BOARD to legacyHome)
        }
        return runCatching {
            val source = JSONObject(raw)
            val next = mutableMapOf<String, List<VocabButton>>()
            source.keys().forEach { boardId ->
                val array = source.getJSONArray(boardId)
                next[boardId] = List(array.length()) { index -> array.getJSONObject(index).toButton() }
            }
            fallback + next
        }.getOrDefault(fallback)
    }

    fun saveBoards(context: Context, boards: Map<String, List<VocabButton>>) {
        val root = JSONObject()
        boards.forEach { (boardId, buttons) ->
            val array = JSONArray()
            buttons.forEach { array.put(it.toJson()) }
            root.put(boardId, array)
        }
        prefs(context).edit()
            .putString(KEY_BOARDS, root.toString())
            .putString(KEY_HOME, JSONArray().also { array ->
                boards[Defaults.HOME_BOARD].orEmpty().forEach { array.put(it.toJson()) }
            }.toString())
            .apply()
    }

    fun restoreDefaultLayout(context: Context) {
        prefs(context).edit().remove(KEY_HOME).remove(KEY_BOARDS).apply()
    }

    fun wipeLearnedHistory(context: Context) {
        prefs(context).edit()
            .remove(KEY_SENTENCES)
            .remove(KEY_COUNTS)
            .remove(KEY_TRANSITIONS)
            .apply()
    }

    fun restoreAllDefaults(context: Context) {
        prefs(context).edit()
            .remove(KEY_HOME)
            .remove(KEY_BOARDS)
            .remove(KEY_SENTENCES)
            .remove(KEY_COUNTS)
            .remove(KEY_TRANSITIONS)
            .remove(KEY_VOICE_NAME)
            .remove(KEY_SPEECH_RATE)
            .apply()
    }

    fun trackWord(context: Context, label: String) {
        val counts = JSONObject(prefs(context).getString(KEY_COUNTS, "{}") ?: "{}")
        counts.put(label, counts.optInt(label, 0) + 1)
        prefs(context).edit().putString(KEY_COUNTS, counts.toString()).apply()
    }

    fun trackTransition(context: Context, previous: String?, next: String) {
        if (previous.isNullOrBlank() || next.isBlank()) return
        val transitions = JSONObject(prefs(context).getString(KEY_TRANSITIONS, "{}") ?: "{}")
        val key = transitionKey(previous, next)
        transitions.put(key, transitions.optInt(key, 0) + 1)
        prefs(context).edit().putString(KEY_TRANSITIONS, transitions.toString()).apply()
    }

    fun trackSentence(context: Context, spoken: String) {
        if (spoken.isBlank()) return
        val now = System.currentTimeMillis()
        val source = JSONArray(prefs(context).getString(KEY_SENTENCES, "[]") ?: "[]")
        val fresh = JSONArray()
        for (index in 0 until source.length()) {
            val item = source.getJSONObject(index)
            if (now - item.optLong("at", 0L) <= THIRTY_DAYS_MS) fresh.put(item)
        }
        fresh.put(JSONObject().put("at", now).put("spoken", spoken))
        prefs(context).edit().putString(KEY_SENTENCES, fresh.toString()).apply()
    }

    fun usageSummary(context: Context): List<Pair<String, Int>> {
        val counts = JSONObject(prefs(context).getString(KEY_COUNTS, "{}") ?: "{}")
        return counts.keys().asSequence()
            .map { it to counts.optInt(it) }
            .sortedByDescending { it.second }
            .take(10)
            .toList()
    }

    fun recommendations(
        context: Context,
        lastWord: String?,
        currentBoard: String?,
        visibleButtons: List<VocabButton>,
        boards: Map<String, List<VocabButton>>,
    ): RecommendationResult {
        val allButtons = (boards.values.flatten() + Defaults.pinned)
            .distinctBy { it.label.normalized() }
            .associateBy { it.label.normalized() }

        val counts = JSONObject(prefs(context).getString(KEY_COUNTS, "{}") ?: "{}")
        val transitions = JSONObject(prefs(context).getString(KEY_TRANSITIONS, "{}") ?: "{}")
        val scores = linkedMapOf<String, RecommendationScore>()

        if (!lastWord.isNullOrBlank()) {
            val from = lastWord.normalized()
            transitions.keys().asSequence()
                .mapNotNull { key ->
                    val split = key.split(">")
                    if (split.size == 2 && split[0] == from) split[1] to transitions.optInt(key) else null
                }
                .sortedByDescending { it.second }
                .forEach { (word, count) ->
                    allButtons[word]?.let { button ->
                        scores[button.label.normalized()] = RecommendationScore(button, count * 10 + 50, "frequent after $lastWord")
                    }
                }
        }

        ruleFallback(lastWord, currentBoard).forEachIndexed { index, label ->
            allButtons[label.normalized()]?.let { button ->
                scores.putIfAbsent(button.label.normalized(), RecommendationScore(button, 40 - index, "common path"))
            }
        }

        visibleButtons.take(5).forEachIndexed { index, button ->
            scores.putIfAbsent(button.label.normalized(), RecommendationScore(button, 25 - index, "on this board"))
        }

        counts.keys().asSequence()
            .mapNotNull { key -> allButtons[key.normalized()]?.let { RecommendationScore(it, counts.optInt(key), "frequently used") } }
            .sortedByDescending { it.score }
            .forEach { scores.putIfAbsent(it.button.label.normalized(), it.copy(score = it.score + 10)) }

        val recommendations = scores.values
            .sortedByDescending { it.score }
            .take(5)
            .map { it.button }

        val totalTaps = counts.keys().asSequence().sumOf { counts.optInt(it) }
        val totalTransitions = transitions.keys().asSequence().sumOf { transitions.optInt(it) }
        val status = when {
            totalTransitions >= 40 -> "learning from regular use"
            totalTransitions >= 12 -> "starting to personalize"
            totalTaps >= 8 -> "collecting patterns"
            else -> "starter suggestions"
        }
        return RecommendationResult(recommendations, status)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun transitionKey(previous: String, next: String) = "${previous.normalized()}>${next.normalized()}"

    private fun String.normalized() = lowercase(Locale.ROOT).trim()

    private fun ruleFallback(lastWord: String?, currentBoard: String?): List<String> {
        return when (lastWord?.normalized()) {
            "i" -> listOf("want", "need", "go", "feel", "like")
            "you" -> listOf("want", "need", "go", "help", "stop")
            "want" -> listOf("food", "drink", "toilet", "play", "help")
            "need" -> listOf("toilet", "help", "drink", "food", "rest")
            "go" -> listOf("home", "school", "toilet", "outside", "shops")
            "food" -> listOf("apple", "banana", "bread", "snack", "finished")
            "drink" -> listOf("water", "juice", "milk", "cup", "finished")
            "feel" -> listOf("happy", "sad", "sick", "tired", "angry")
            else -> when (currentBoard) {
                "want" -> listOf("food", "drink", "toilet", "play", "help")
                "need" -> listOf("toilet", "help", "drink", "food", "rest")
                "go" -> listOf("home", "school", "toilet", "outside", "shops")
                else -> listOf("I", "want", "need", "toilet", "help")
            }
        }
    }

    private fun VocabButton.toJson() = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("speech", speech)
        .put("icon", icon)
        .put("color", color)
        .put("boardId", boardId)
        .put("isCategory", isCategory)

    private fun JSONObject.toButton() = VocabButton(
        id = optString("id"),
        label = optString("label"),
        speech = optString("speech", optString("label")),
        icon = optString("icon", "□"),
        color = optLong("color", 0xFFFFFFFF),
        boardId = optString("boardId").ifBlank { null },
        isCategory = optBoolean("isCategory", false),
    )
}

private data class RecommendationScore(
    val button: VocabButton,
    val score: Int,
    val reason: String,
)

private data class RecommendationResult(
    val buttons: List<VocabButton>,
    val status: String,
)

private object Defaults {
    const val HOME_BOARD = "home"

    val pinned = listOf(
        VocabButton("pin_yes", "yes", "yes", "✓", 0xFFA8E6A1),
        VocabButton("pin_no", "no", "no", "✕", 0xFFFFB3A7),
        VocabButton("pin_more", "more", "more", "+", 0xFFFFE08A),
        VocabButton("pin_help", "help", "help", "?", 0xFFFFC36E),
        VocabButton("pin_stop", "stop", "stop", "STOP", 0xFFFF6B6B),
    )

    val home = listOf(
        VocabButton("i", "I", "I", "☝", 0xFFFFFFFF, "i", true),
        VocabButton("you", "you", "you", "👤", 0xFFFFFFFF, "you", true),
        VocabButton("want", "want", "want", "★", 0xFFFFF3A3, "want", true),
        VocabButton("need", "need", "need", "!", 0xFFFFD1A8, "need", true),
        VocabButton("go", "go", "go", "→", 0xFFB8F5C7, "go", true),
        VocabButton("food", "food", "food", "🍎", 0xFFFFDFA6, "food", true),
        VocabButton("drink", "drink", "drink", "🥤", 0xFFAEE8FF, "drink", true),
        VocabButton("toilet", "toilet", "toilet", "🚽", 0xFFD8E6FF, "toilet", true),
        VocabButton("people", "people", "people", "●●", 0xFFFFC98F, "people", true),
        VocabButton("places", "places", "places", "⌂", 0xFFBDE8C9, "places", true),
        VocabButton("home", "home", "home", "⌂", 0xFFBDE8C9),
        VocabButton("school", "school", "school", "▣", 0xFFCDEB91),
        VocabButton("play", "play", "play", "▶", 0xFFD7B5FF, "play", true),
        VocabButton("feel", "feel", "feel", "☺", 0xFFFFC6E3, "feel", true),
        VocabButton("body", "body", "body", "✋", 0xFFAEE8FF, "body", true),
        VocabButton("like", "like", "like", "♡", 0xFFFFF3A3),
        VocabButton("dont", "don't", "don't", "✕", 0xFFFFB3A7),
        VocabButton("have", "have", "have", "▣", 0xFFFFFFFF),
        VocabButton("things", "things", "things", "□", 0xFFE0E0E0, "things", true),
        VocabButton("finished", "finished", "finished", "✓", 0xFFCFCFCF),
    )

    val boards: Map<String, List<VocabButton>> = mapOf(
        "i" to listOf(category("want", "★"), category("need", "!"), category("go", "→"), category("feel", "☺"), word("like")),
        "you" to listOf(category("want", "★"), category("need", "!"), category("go", "→"), word("help"), word("stop")),
        "want" to listOf(category("food", "🍎"), category("drink", "🥤"), category("play", "▶"), category("toilet", "🚽"), word("help")),
        "need" to listOf(category("toilet", "🚽"), word("help"), category("drink", "🥤"), category("food", "🍎"), word("rest")),
        "go" to listOf(word("home", icon = "⌂"), word("school", icon = "▣"), category("toilet", "🚽"), word("outside", icon = "☀"), word("shops", icon = "$")),
        "food" to listOf(word("apple", icon = "🍎"), word("banana", icon = "🍌"), word("bread", icon = "▭"), word("snack", icon = "□"), word("finished")),
        "drink" to listOf(word("water", icon = "💧"), word("juice", icon = "🥤"), word("milk", icon = "◯"), word("cup", icon = "∪"), word("finished")),
        "toilet" to listOf(word("toilet", icon = "🚽"), word("bathroom", icon = "🚪"), word("wash", icon = "💧"), word("now", icon = "!"), word("finished")),
        "people" to listOf(word("Mum", icon = "●"), word("Dad", icon = "●"), word("friend", icon = "●●"), word("teacher", icon = "□"), word("me", icon = "☝")),
        "places" to listOf(word("home", icon = "⌂"), word("school", icon = "▣"), category("toilet", "🚽"), word("outside", icon = "☀"), word("shops", icon = "$")),
        "play" to listOf(word("game", icon = "▶"), word("toy", icon = "★"), word("music", icon = "♪"), word("book", icon = "▤"), word("finished")),
        "feel" to listOf(word("happy", icon = "☺"), word("sad", icon = "☹"), word("sick", icon = "+"), word("tired", icon = "z"), word("angry", icon = "!")),
        "body" to listOf(word("head", icon = "○"), word("hand", icon = "✋"), word("mouth", icon = "◡"), word("tummy", icon = "○"), word("hurts", icon = "!")),
        "things" to listOf(word("toy", icon = "★"), word("blanket", icon = "▭"), word("tablet", icon = "▣"), word("book", icon = "▤"), word("bag", icon = "□")),
    )

    private fun word(
        label: String,
        icon: String = "□",
        boardId: String? = null,
    ) = VocabButton(
        id = label.lowercase(Locale.ROOT).replace(" ", "_"),
        label = label,
        speech = label,
        icon = icon,
        color = 0xFFFFFFFF,
        boardId = boardId,
        isCategory = boardId != null,
    )

    private fun category(label: String, icon: String = "□") = word(label, icon, label)

    fun allBoards(): Map<String, List<VocabButton>> = boards + (HOME_BOARD to home)
}

private fun String.normalizedId(): String =
    lowercase(Locale.ROOT)
        .trim()
        .replace(" ", "_")
        .filter { it.isLetterOrDigit() || it == '_' }

private enum class DropAction {
    MoveBefore,
    MoveAfter,
    MoveIntoFolder,
}

private data class DropPreview(
    val targetIndex: Int,
    val action: DropAction,
)

private class FolderShape(
    private val cornerRadius: Dp = 8.dp,
    private val tabWidthPercent: Float = 0.30f,
    private val tabHeight: Dp = 10.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = cornerRadius.value * density.density
        val tabW = size.width * tabWidthPercent
        val tabH = kotlin.math.min(size.height * 0.20f, tabHeight.value * density.density)
        val path = Path().apply {
            moveTo(0f, size.height - r)
            quadraticTo(0f, size.height, r, size.height)
            lineTo(size.width - r, size.height)
            quadraticTo(size.width, size.height, size.width, size.height - r)
            lineTo(size.width, tabH + r)
            quadraticTo(size.width, tabH, size.width - r, tabH)
            lineTo(tabW, tabH)
            lineTo(tabW, r)
            quadraticTo(tabW, 0f, tabW - r, 0f)
            lineTo(r, 0f)
            quadraticTo(0f, 0f, 0f, r)
            lineTo(0f, tabH)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun OpenAacApp() {
    val context = LocalContext.current
    var setupComplete by remember { mutableStateOf(Store.isSetup(context)) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var voiceOptions by remember { mutableStateOf(emptyList<VoiceOption>()) }
    var selectedVoiceName by remember { mutableStateOf(Store.voiceName(context)) }
    var speechRate by remember { mutableStateOf(Store.speechRate(context)) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        tts = engine
        onDispose { engine.shutdown() }
    }

    LaunchedEffect(ttsReady, selectedVoiceName, speechRate) {
        val engine = tts ?: return@LaunchedEffect
        if (!ttsReady) return@LaunchedEffect
        val localEnglishVoices = engine.voices
            ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
            ?.sortedWith(compareByDescending<Voice> { it.locale.country == "AU" }.thenBy { it.locale.displayName }.thenBy { it.name })
            .orEmpty()
        voiceOptions = localEnglishVoices.map { voice ->
            VoiceOption(voice.name, "${voice.locale.displayName} (${voice.name.takeLast(10)})")
        }
        val targetName = selectedVoiceName
            ?: localEnglishVoices.firstOrNull { it.locale.country == "AU" }?.name
            ?: localEnglishVoices.firstOrNull()?.name
        val targetVoice = localEnglishVoices.firstOrNull { it.name == targetName }
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

    val speak: (String) -> Unit = { text ->
        if (ttsReady && text.isNotBlank()) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    if (!setupComplete) {
        SetupScreen(onComplete = {
            Store.setup(context, it)
            setupComplete = true
        })
    } else {
        CommunicatorScreen(
            speak = speak,
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
private fun SetupScreen(onComplete: (String) -> Unit) {
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

@Composable
private fun CommunicatorScreen(
    speak: (String) -> Unit,
    voiceOptions: List<VoiceOption>,
    selectedVoiceName: String?,
    speechRate: Float,
    onVoiceSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    val sentence = remember { mutableStateListOf<SentenceToken>() }
    val boardStack = remember { mutableStateListOf<String>() }
    var boardsById by remember { mutableStateOf(Store.boards(context)) }
    var draftBoards by remember { mutableStateOf(boardsById) }
    var showAdminLogin by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var reorganizing by remember { mutableStateOf(false) }
    var showAddButton by remember { mutableStateOf(false) }
    var editingButtonIndex by remember { mutableStateOf<Int?>(null) }
    var recommendationRefresh by remember { mutableStateOf(0) }
    var usageRefresh by remember { mutableStateOf(0) }

    val currentBoard = boardStack.lastOrNull()
    val currentBoardId = currentBoard ?: Defaults.HOME_BOARD
    val activeBoards = if (reorganizing) draftBoards else boardsById
    val buttons = activeBoards[currentBoardId].orEmpty()
    val recommendationResult = remember(currentBoard, boardsById, sentence.size, recommendationRefresh) {
        Store.recommendations(
            context = context,
            lastWord = sentence.lastOrNull()?.label,
            currentBoard = currentBoard,
            visibleButtons = buttons,
            boards = boardsById,
        )
    }

    fun selectButton(button: VocabButton) {
        if (reorganizing) {
            button.boardId?.let { boardStack.add(it) }
            return
        }
        val previous = sentence.lastOrNull()?.label
        sentence.add(SentenceToken(button.label, button.speech, button.icon))
        speak(button.speech)
        Store.trackWord(context, button.label)
        Store.trackTransition(context, previous, button.label)
        recommendationRefresh++
        button.boardId?.let { boardStack.add(it) }
    }

    fun updateDraftBoard(boardId: String, nextButtons: List<VocabButton>) {
        draftBoards = draftBoards + (boardId to nextButtons.take(20))
    }

    fun moveDraftButton(fromIndex: Int, toIndex: Int, action: DropAction) {
        val boardButtons = draftBoards[currentBoardId].orEmpty()
        if (fromIndex !in boardButtons.indices || toIndex !in 0 until 20 || fromIndex == toIndex) return
        val target = boardButtons.getOrNull(toIndex)
        val moved = boardButtons[fromIndex]
        if (action == DropAction.MoveIntoFolder && target != null && target.boardId != null && target.id != moved.id) {
            val nextBoards = draftBoards.toMutableMap()
            nextBoards[currentBoardId] = boardButtons.filterIndexed { index, _ -> index != fromIndex }
            nextBoards[target.boardId] = (nextBoards[target.boardId].orEmpty() + moved).take(20)
            draftBoards = nextBoards
        } else {
            val next = boardButtons.toMutableList()
            val item = next.removeAt(fromIndex)
            val insertionIndex = if (action == DropAction.MoveAfter) toIndex + 1 else toIndex
            val adjustedIndex = if (fromIndex < insertionIndex) insertionIndex - 1 else insertionIndex
            next.add(adjustedIndex.coerceIn(0, next.size), item)
            updateDraftBoard(currentBoardId, next)
        }
    }

    fun addDraftButton(button: VocabButton) {
        val nextBoards = draftBoards.toMutableMap()
        nextBoards[currentBoardId] = (nextBoards[currentBoardId].orEmpty() + button).take(20)
        button.boardId?.let { boardId ->
            nextBoards.putIfAbsent(boardId, emptyList())
        }
        draftBoards = nextBoards
    }

    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (reorganizing) {
                ReorganizeBar(
                    boardName = if (currentBoardId == Defaults.HOME_BOARD) "home" else currentBoardId,
                    canGoHome = currentBoard != null,
                    canGoBack = boardStack.isNotEmpty(),
                    onHome = { boardStack.clear() },
                    onBack = { if (boardStack.isNotEmpty()) boardStack.removeAt(boardStack.lastIndex) },
                    onAdd = { showAddButton = true },
                    onCancel = {
                        draftBoards = boardsById
                        boardStack.clear()
                        reorganizing = false
                    },
                    onDone = {
                        boardsById = draftBoards
                        Store.saveBoards(context, draftBoards)
                        boardStack.clear()
                        reorganizing = false
                        recommendationRefresh++
                    },
                )
            } else {
                SentenceBar(
                    sentence = sentence,
                    onSpeak = {
                        val spoken = sentence.joinToString(" ") { it.speech }
                        speak(spoken)
                        Store.trackSentence(context, spoken)
                    },
                    onBackspace = { if (sentence.isNotEmpty()) sentence.removeAt(sentence.lastIndex) },
                    onClear = { sentence.clear() },
                    onQuestion = {
                        val previous = sentence.lastOrNull()?.label
                        sentence.add(SentenceToken("?", "hmm", "?"))
                        speak("hmm")
                        Store.trackWord(context, "hmm")
                        Store.trackTransition(context, previous, "hmm")
                        recommendationRefresh++
                    },
                    onAdmin = { showAdminLogin = true },
                )
            }

            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavButton("home", enabled = currentBoard != null) { boardStack.clear() }
                    NavButton("back", enabled = boardStack.isNotEmpty()) {
                        if (boardStack.isNotEmpty()) boardStack.removeAt(boardStack.lastIndex)
                    }
                }

                ButtonGrid(
                    buttons = buttons,
                    modifier = Modifier.weight(1f),
                    onTap = ::selectButton,
                    reorganizing = reorganizing,
                    onMove = ::moveDraftButton,
                    onEdit = { editingButtonIndex = it },
                )

                if (!reorganizing) {
                    SuggestionsPanel(
                        result = recommendationResult,
                        onTap = ::selectButton,
                    )
                }
            }

            if (!reorganizing) {
                PinnedStrip(
                    onTap = ::selectButton,
                )
            }
        }
    }

    if (showAdminLogin) {
        AdminLogin(
            onDismiss = { showAdminLogin = false },
            onSuccess = {
                showAdminLogin = false
                showAdmin = true
            },
        )
    }

    if (showAdmin) {
        AdminScreen(
            onRestoreLayout = {
                Store.restoreDefaultLayout(context)
                boardsById = Defaults.allBoards()
                draftBoards = boardsById
            },
            onRestoreAll = {
                Store.restoreAllDefaults(context)
                boardsById = Defaults.allBoards()
                draftBoards = boardsById
                sentence.clear()
                boardStack.clear()
                recommendationRefresh++
                usageRefresh++
                onSpeechRateChanged(1.0f)
                voiceOptions.firstOrNull()?.let { onVoiceSelected(it.name) }
            },
            onWipeLearning = {
                Store.wipeLearnedHistory(context)
                recommendationRefresh++
                usageRefresh++
            },
            voiceOptions = voiceOptions,
            selectedVoiceName = selectedVoiceName,
            speechRate = speechRate,
            onVoiceSelected = onVoiceSelected,
            onSpeechRateChanged = onSpeechRateChanged,
            onTestVoice = { speak("I want food") },
            usageRefresh = usageRefresh,
            onEnterReorganize = {
                draftBoards = boardsById
                boardStack.clear()
                showAdmin = false
                reorganizing = true
            },
            onClose = { showAdmin = false },
        )
    }

    if (showAddButton) {
        AddButtonDialog(
            boards = draftBoards,
            onDismiss = { showAddButton = false },
            onAdd = {
                addDraftButton(it)
                showAddButton = false
            },
        )
    }

    editingButtonIndex?.let { index ->
        draftBoards[currentBoardId]?.getOrNull(index)?.let { item ->
            EditButtonDialog(
                item = item,
                onDismiss = { editingButtonIndex = null },
                onSave = { updated ->
                    val nextBoards = draftBoards.toMutableMap()
                    val nextButtons = nextBoards[currentBoardId].orEmpty().toMutableList()
                    if (index in nextButtons.indices) {
                        nextButtons[index] = updated
                        nextBoards[currentBoardId] = nextButtons
                        updated.boardId?.let { nextBoards.putIfAbsent(it, emptyList()) }
                        draftBoards = nextBoards
                    }
                    editingButtonIndex = null
                },
            )
        } ?: run {
            editingButtonIndex = null
        }
    }
}

@Composable
private fun ReorganizeBar(
    boardName: String,
    canGoHome: Boolean,
    canGoBack: Boolean,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF7DA))
            .border(2.dp, Color(0xFFFFC94D), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Reorganizing", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D3420))
            Text("Board: $boardName", fontSize = 18.sp, color = Color(0xFF6B5300), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(onClick = onHome, enabled = canGoHome, modifier = Modifier.height(70.dp)) { Text("home") }
        OutlinedButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.height(70.dp)) { Text("back") }
        Button(onClick = onAdd, modifier = Modifier.height(70.dp)) { Text("add") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.height(70.dp)) { Text("cancel") }
        Button(onClick = onDone, modifier = Modifier.height(70.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2166F3))) {
            Text("done")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SentenceBar(
    sentence: List<SentenceToken>,
    onSpeak: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onQuestion: () -> Unit,
    onAdmin: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFD7E0EA), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sentence.takeLast(8).forEach {
                SentenceChip(it)
            }
        }
        ActionButton("speak", "▶", Color(0xFF2166F3), onSpeak)
        BackspaceButton(onBackspace = onBackspace, onClear = onClear)
        ActionButton("hmm", "?", Color(0xFFFFD36E), onQuestion)
        OutlinedButton(onClick = onAdmin, modifier = Modifier.height(70.dp)) {
            Text("admin", fontSize = 15.sp)
        }
    }
}

@Composable
private fun SentenceChip(token: SentenceToken) {
    Column(
        Modifier
            .width(82.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF2F6FA))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(token.icon, fontSize = 24.sp, maxLines = 1)
        Text(token.label, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ButtonGrid(
    buttons: List<VocabButton>,
    modifier: Modifier,
    onTap: (VocabButton) -> Unit,
    reorganizing: Boolean = false,
    onMove: (Int, Int, DropAction) -> Unit = { _, _, _ -> },
    onEdit: (Int) -> Unit = {},
) {
    var dropPreview by remember { mutableStateOf<DropPreview?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = buttons.chunked(5).take(4)
        rows.forEachIndexed { rowIndex, row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { columnIndex, button ->
                    val index = rowIndex * 5 + columnIndex
                    ReorderableVocabTile(
                        button = button,
                        index = index,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        reorganizing = reorganizing,
                        onTap = onTap,
                        onMove = onMove,
                        onEdit = onEdit,
                        buttons = buttons,
                        dropPreview = dropPreview?.takeIf { it.targetIndex == index },
                        onDropPreview = { dropPreview = it },
                    )
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        repeat(4 - rows.size) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ReorderableVocabTile(
    button: VocabButton,
    index: Int,
    modifier: Modifier,
    reorganizing: Boolean,
    onTap: (VocabButton) -> Unit,
    onMove: (Int, Int, DropAction) -> Unit,
    onEdit: (Int) -> Unit,
    buttons: List<VocabButton>,
    dropPreview: DropPreview?,
    onDropPreview: (DropPreview?) -> Unit,
) {
    val wiggle = rememberInfiniteTransition(label = "tile-wiggle")
    val rotation by wiggle.animateFloat(
        initialValue = -0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(animation = tween(320), repeatMode = RepeatMode.Reverse),
        label = "rotation",
    )
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    val dragModifier = if (reorganizing) {
        Modifier.pointerInput(index) {
            fun currentDropPreview(): DropPreview {
                val tileWidth = size.width.toFloat().coerceAtLeast(1f)
                val tileHeight = size.height.toFloat().coerceAtLeast(1f)
                val fromRow = index / 5
                val fromColumn = index % 5
                val projectedCenterX = ((fromColumn + 0.5f) * tileWidth) + dragX
                val projectedCenterY = ((fromRow + 0.5f) * tileHeight) + dragY
                val targetColumn = kotlin.math.floor(projectedCenterX / tileWidth).toInt().coerceIn(0, 4)
                val targetRow = kotlin.math.floor(projectedCenterY / tileHeight).toInt().coerceIn(0, 3)
                val targetIndex = targetRow * 5 + targetColumn
                val target = buttons.getOrNull(targetIndex)
                val cellX = projectedCenterX - (targetColumn * tileWidth)
                val folderDropAction = when {
                    cellX < tileWidth * 0.28f -> DropAction.MoveBefore
                    cellX > tileWidth * 0.72f -> DropAction.MoveAfter
                    else -> DropAction.MoveIntoFolder
                }
                val action = if (target?.boardId != null && target.id != button.id) {
                    folderDropAction
                } else {
                    DropAction.MoveBefore
                }
                return DropPreview(targetIndex = targetIndex, action = action)
            }
            detectDragGestures(
                onDragEnd = {
                    val preview = currentDropPreview()
                    onMove(index, preview.targetIndex, preview.action)
                    onDropPreview(null)
                    dragX = 0f
                    dragY = 0f
                },
                onDragCancel = {
                    onDropPreview(null)
                    dragX = 0f
                    dragY = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragX += dragAmount.x
                    dragY += dragAmount.y
                    onDropPreview(currentDropPreview())
                },
            )
        }
    } else {
        Modifier
    }
    VocabTile(
        button = button,
        modifier = modifier
            .then(dragModifier)
            .graphicsLayer {
                rotationZ = if (reorganizing) rotation else 0f
                translationX = if (reorganizing) dragX else 0f
                translationY = if (reorganizing) dragY else 0f
                shadowElevation = if (reorganizing && (dragX != 0f || dragY != 0f)) 10f else 0f
            },
        onTap = onTap,
        onLongPress = if (reorganizing) ({ onEdit(index) }) else null,
        dropPreview = dropPreview?.action,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VocabTile(
    button: VocabButton,
    modifier: Modifier,
    onTap: (VocabButton) -> Unit,
    onLongPress: (() -> Unit)? = null,
    dropPreview: DropAction? = null,
) {
    val isFolder = button.isCategory || button.boardId != null
    val shape = if (isFolder) FolderShape() else RoundedCornerShape(12.dp)
    val borderColor = when (dropPreview) {
        DropAction.MoveIntoFolder -> Color(0xFF1F9D55)
        DropAction.MoveBefore,
        DropAction.MoveAfter -> Color(0xFF2166F3)
        null -> if (isFolder) Color(0xFFFFC94D) else Color(button.color)
    }
    val borderWidth = if (dropPreview != null) 5.dp else 3.dp
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (dropPreview == DropAction.MoveIntoFolder) Color(0xFFE9F9EF) else Color.White)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .combinedClickable(onClick = { onTap(button) }, onLongClick = onLongPress),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(
                start = 4.dp,
                end = 4.dp,
                top = if (isFolder) 14.dp else 6.dp,
                bottom = 6.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = button.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = button.icon,
                fontSize = if (button.icon.length > 3) 18.sp else 28.sp,
                color = Color.Black,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        dropPreview?.let { preview ->
            val label = when (preview) {
                DropAction.MoveBefore -> "before"
                DropAction.MoveAfter -> "after"
                DropAction.MoveIntoFolder -> "into"
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(if (preview == DropAction.MoveIntoFolder) Color(0xFF1F9D55) else Color(0xFF2166F3))
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PinnedStrip(onTap: (VocabButton) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Defaults.pinned.forEach { button ->
            VocabTile(button, Modifier.weight(1f).fillMaxHeight(), onTap)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionsPanel(result: RecommendationResult, onTap: (VocabButton) -> Unit) {
    Column(
        Modifier
            .width(138.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEAF0F7))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("quick", fontWeight = FontWeight.Bold, color = Color(0xFF56616F))
        Text(result.status, fontSize = 11.sp, color = Color(0xFF56616F), lineHeight = 12.sp)
        result.buttons.forEach { button ->
            val isFolder = button.isCategory || button.boardId != null
            val shape = if (isFolder) FolderShape(cornerRadius = 8.dp) else RoundedCornerShape(8.dp)
            val borderColor = if (isFolder) Color(0xFFFFC94D) else Color(button.color)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(shape)
                    .background(Color.White)
                    .border(2.dp, borderColor, shape)
                    .combinedClickable(onClick = { onTap(button) }),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(
                        start = 4.dp,
                        end = 4.dp,
                        top = if (isFolder) 9.dp else 4.dp,
                        bottom = 4.dp,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        button.label,
                        fontSize = 13.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        button.icon,
                        fontSize = if (button.icon.length > 3) 13.sp else 18.sp,
                        color = Color.Black,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(76.dp)) {
        Text(label, fontSize = 18.sp)
    }
}

@Composable
private fun ActionButton(label: String, icon: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(78.dp).height(70.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 24.sp, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color.White, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(onBackspace: () -> Unit, onClear: () -> Unit) {
    Box(
        Modifier
            .width(78.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF5D6673))
            .combinedClickable(onClick = onBackspace, onLongClick = onClear),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⌫", fontSize = 24.sp, color = Color.White)
            Text("delete", fontSize = 11.sp, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun AdminLogin(onDismiss: () -> Unit, onSuccess: () -> Unit) {
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
private fun AdminScreen(
    onRestoreLayout: () -> Unit,
    onRestoreAll: () -> Unit,
    onWipeLearning: () -> Unit,
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
    val usage = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(usageRefresh) {
        usage.clear()
        Store.usageSummary(context).forEach { usage[it.first] = it.second }
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
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Board editing", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Use reorganize board to return to the communication screen, keep folders navigable, drag words into place, add words, and save with Done.",
                            fontSize = 17.sp,
                            color = Color(0xFF56616F),
                        )
                        Button(onClick = onEnterReorganize, modifier = Modifier.height(58.dp)) {
                            Text("start reorganizing")
                        }
                        OutlinedButton(onClick = onWipeLearning) { Text("wipe learned history") }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoiceControls(
                            voiceOptions = voiceOptions,
                            selectedVoiceName = selectedVoiceName,
                            speechRate = speechRate,
                            onVoiceSelected = onVoiceSelected,
                            onSpeechRateChanged = onSpeechRateChanged,
                            onTestVoice = onTestVoice,
                        )
                        Text("Local usage", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (usage.isEmpty()) Text("No words tracked yet.")
                        usage.forEach { (word, count) ->
                            Text("$word: $count", fontSize = 17.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceControls(
    voiceOptions: List<VoiceOption>,
    selectedVoiceName: String?,
    speechRate: Float,
    onVoiceSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onTestVoice: () -> Unit,
) {
    val currentIndex = voiceOptions.indexOfFirst { it.name == selectedVoiceName }.takeIf { it >= 0 } ?: 0
    val current = voiceOptions.getOrNull(currentIndex)
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F6FA))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Voice", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(current?.label ?: "Default Android English voice", fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = voiceOptions.size > 1,
                    onClick = {
                        val previous = if (currentIndex <= 0) voiceOptions.lastIndex else currentIndex - 1
                        voiceOptions.getOrNull(previous)?.let { onVoiceSelected(it.name) }
                    },
                ) { Text("prev") }
                OutlinedButton(
                    enabled = voiceOptions.size > 1,
                    onClick = {
                        val next = if (currentIndex >= voiceOptions.lastIndex) 0 else currentIndex + 1
                        voiceOptions.getOrNull(next)?.let { onVoiceSelected(it.name) }
                    },
                ) { Text("next") }
                Button(onClick = onTestVoice) { Text("test") }
            }
            Text("Speed ${(speechRate * 100).toInt()}%", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSpeechRateChanged(speechRate - 0.1f) }) { Text("slower") }
                OutlinedButton(onClick = { onSpeechRateChanged(speechRate + 0.1f) }) { Text("faster") }
            }
        }
    }
}

@Composable
private fun AddButtonDialog(
    boards: Map<String, List<VocabButton>>,
    onDismiss: () -> Unit,
    onAdd: (VocabButton) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var speech by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("□") }
    var createFolder by remember { mutableStateOf(false) }
    val allButtons = remember(boards) {
        boards.values.flatten()
            .distinctBy { "${it.label.normalizedId()}|${it.boardId.orEmpty()}|${it.speech}" }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }
    val matches = remember(query, allButtons) {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) emptyList() else allButtons.filter {
            it.label.lowercase(Locale.ROOT).contains(normalized) ||
                it.speech.lowercase(Locale.ROOT).contains(normalized) ||
                it.boardId.orEmpty().lowercase(Locale.ROOT).contains(normalized)
        }.take(6)
    }

    FullScreenOverlay {
        Card(Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.88f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier
                    .imePadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Add button", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(24)
                        if (label.isBlank()) {
                            label = it.take(18)
                            speech = it.take(32)
                        }
                    },
                    label = { Text("Search words and folders") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (matches.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxWidth().height(174.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(matches) { _, button ->
                            val isFolder = button.boardId != null
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF2F6FA))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(button.icon, fontSize = 24.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                Column(Modifier.weight(1f)) {
                                    Text(button.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    Text(if (isFolder) "folder" else button.speech, fontSize = 13.sp, color = Color(0xFF56616F), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Button(onClick = {
                                    keyboard?.hide()
                                    onAdd(button.copy(id = "copy_${button.id}_${System.currentTimeMillis()}"))
                                }) { Text("add") }
                            }
                        }
                    }
                } else {
                    Text("No matches yet. Create a new button below.", fontSize = 14.sp, color = Color(0xFF56616F))
                }

                Text("Create new", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(label, {
                    label = it.take(18)
                    if (speech.isBlank()) speech = it.take(32)
                }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(speech, { speech = it.take(32) }, label = { Text("Speech") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Icon text") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { createFolder = !createFolder },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (createFolder) Color(0xFFFFF7DA) else Color.Transparent),
                    ) {
                        Text(if (createFolder) "folder on" else "make folder")
                    }
                    Text("Folders open a new board and can receive dropped buttons.", fontSize = 13.sp, color = Color(0xFF56616F))
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = {
                        keyboard?.hide()
                        onDismiss()
                    }) { Text("cancel") }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        enabled = label.isNotBlank(),
                        onClick = {
                            val cleanLabel = label.ifBlank { query.ifBlank { "new" } }.take(18)
                            val boardId = if (createFolder) cleanLabel.normalizedId().ifBlank { "folder_${System.currentTimeMillis()}" } else null
                            keyboard?.hide()
                            onAdd(
                                VocabButton(
                                    id = "custom_${System.currentTimeMillis()}",
                                    label = cleanLabel,
                                    speech = speech.ifBlank { cleanLabel },
                                    icon = icon.ifBlank { "□" },
                                    color = if (createFolder) 0xFFFFF3A3 else 0xFFFFFFFF,
                                    boardId = boardId,
                                    isCategory = boardId != null,
                                )
                            )
                        },
                    ) { Text("create") }
                }
            }
        }
    }
}

@Composable
private fun EditButtonDialog(item: VocabButton, onDismiss: () -> Unit, onSave: (VocabButton) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var label by remember(item.id) { mutableStateOf(item.label) }
    var speech by remember(item.id) { mutableStateOf(item.speech) }
    var icon by remember(item.id) { mutableStateOf(item.icon) }
    var folderPath by remember(item.id) { mutableStateOf(item.boardId.orEmpty()) }

    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Edit word", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(label, { label = it.take(18) }, label = { Text("Label") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(speech, { speech = it.take(32) }, label = { Text("Speech") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Icon text") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(folderPath, { folderPath = it.lowercase(Locale.ROOT).filter { char -> char.isLetterOrDigit() || char == '_' }.take(20) }, label = { Text("Folder path, blank for normal word") })
                Text("Examples: want, need, food, drink, toilet, people, places, play, feel, body, things", fontSize = 12.sp, color = Color(0xFF56616F), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        keyboard?.hide()
                        onDismiss()
                    }) { Text("cancel") }
                    Button(onClick = {
                        val normalizedPath = folderPath.ifBlank { null }
                        keyboard?.hide()
                        onSave(
                            item.copy(
                                label = label.ifBlank { item.label },
                                speech = speech.ifBlank { label },
                                icon = icon.ifBlank { "□" },
                                boardId = normalizedPath,
                                isCategory = normalizedPath != null,
                            )
                        )
                    }) { Text("save") }
                }
            }
        }
    }
}

@Composable
private fun FullScreenOverlay(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
            .background(Color(0x99000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
