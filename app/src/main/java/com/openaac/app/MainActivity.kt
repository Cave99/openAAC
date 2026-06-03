package com.openaac.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F7FA)) {
                    OpenAacApp()
                }
            }
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

private object Store {
    private const val PREFS = "openaac"
    private const val KEY_SETUP = "setup_complete"
    private const val KEY_PASSCODE = "admin_passcode"
    private const val KEY_HOME = "home_buttons"
    private const val KEY_SENTENCES = "sentence_history"
    private const val KEY_COUNTS = "usage_counts"
    private const val KEY_TRANSITIONS = "transition_counts"
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

    fun restoreDefaultLayout(context: Context) {
        prefs(context).edit().remove(KEY_HOME).apply()
    }

    fun restoreAllDefaults(context: Context) {
        prefs(context).edit()
            .remove(KEY_HOME)
            .remove(KEY_SENTENCES)
            .remove(KEY_COUNTS)
            .remove(KEY_TRANSITIONS)
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
        homeButtons: List<VocabButton>,
    ): RecommendationResult {
        val allButtons = (homeButtons + Defaults.pinned + Defaults.boards.values.flatten())
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
        "i" to listOf(word("want"), word("need"), word("go", boardId = "go"), word("feel", boardId = "feel"), word("like")),
        "you" to listOf(word("want"), word("need"), word("go", boardId = "go"), word("help"), word("stop")),
        "want" to listOf(word("food", icon = "🍎", boardId = "food"), word("drink", icon = "🥤", boardId = "drink"), word("play", boardId = "play"), word("toilet", icon = "🚽", boardId = "toilet"), word("help")),
        "need" to listOf(word("toilet", icon = "🚽", boardId = "toilet"), word("help"), word("drink", icon = "🥤", boardId = "drink"), word("food", icon = "🍎", boardId = "food"), word("rest")),
        "go" to listOf(word("home", icon = "⌂"), word("school", icon = "▣"), word("toilet", icon = "🚽"), word("outside", icon = "☀"), word("shops", icon = "$")),
        "food" to listOf(word("apple", icon = "🍎"), word("banana", icon = "🍌"), word("bread", icon = "▭"), word("snack", icon = "□"), word("finished")),
        "drink" to listOf(word("water", icon = "💧"), word("juice", icon = "🥤"), word("milk", icon = "◯"), word("cup", icon = "∪"), word("finished")),
        "toilet" to listOf(word("toilet", icon = "🚽"), word("bathroom", icon = "🚪"), word("wash", icon = "💧"), word("now", icon = "!"), word("finished")),
        "people" to listOf(word("Mum", icon = "●"), word("Dad", icon = "●"), word("friend", icon = "●●"), word("teacher", icon = "□"), word("me", icon = "☝")),
        "places" to listOf(word("home", icon = "⌂"), word("school", icon = "▣"), word("toilet", icon = "🚽"), word("outside", icon = "☀"), word("shops", icon = "$")),
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
}

@Composable
private fun OpenAacApp() {
    val context = LocalContext.current
    var setupComplete by remember { mutableStateOf(Store.isSetup(context)) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        val voices = engine.voices
        val auVoice = voices?.firstOrNull { it.locale.language == "en" && it.locale.country == "AU" && !it.isNetworkConnectionRequired }
        if (auVoice != null) engine.voice = auVoice else engine.language = Locale.ENGLISH
        engine.setSpeechRate(1.0f)
        tts = engine
        onDispose { engine.shutdown() }
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
        CommunicatorScreen(speak = speak)
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
private fun CommunicatorScreen(speak: (String) -> Unit) {
    val context = LocalContext.current
    val sentence = remember { mutableStateListOf<SentenceToken>() }
    val boardStack = remember { mutableStateListOf<String>() }
    var homeButtons by remember { mutableStateOf(Store.homeButtons(context)) }
    var showAdminLogin by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var recommendationRefresh by remember { mutableStateOf(0) }

    val currentBoard = boardStack.lastOrNull()
    val buttons = currentBoard?.let { Defaults.boards[it] } ?: homeButtons
    val recommendationResult = remember(currentBoard, homeButtons, sentence.size, recommendationRefresh) {
        Store.recommendations(
            context = context,
            lastWord = sentence.lastOrNull()?.label,
            currentBoard = currentBoard,
            visibleButtons = buttons,
            homeButtons = homeButtons,
        )
    }

    fun selectButton(button: VocabButton) {
        val previous = sentence.lastOrNull()?.label
        sentence.add(SentenceToken(button.label, button.speech, button.icon))
        speak(button.speech)
        Store.trackWord(context, button.label)
        Store.trackTransition(context, previous, button.label)
        recommendationRefresh++
        button.boardId?.let { boardStack.add(it) }
    }

    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    sentence.add(SentenceToken("?", "hmm", "?"))
                    speak("hmm")
                    Store.trackWord(context, "hmm")
                },
                onAdmin = { showAdminLogin = true },
            )

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
                )

                SuggestionsPanel(
                    result = recommendationResult,
                    onTap = ::selectButton,
                )
            }

            PinnedStrip(
                onTap = ::selectButton,
            )
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
            homeButtons = homeButtons,
            onButtonsChanged = {
                homeButtons = it
                Store.saveHomeButtons(context, it)
            },
            onRestoreLayout = {
                Store.restoreDefaultLayout(context)
                homeButtons = Defaults.home
            },
            onRestoreAll = {
                Store.restoreAllDefaults(context)
                homeButtons = Defaults.home
                sentence.clear()
                boardStack.clear()
            },
            onClose = { showAdmin = false },
        )
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
private fun ButtonGrid(buttons: List<VocabButton>, modifier: Modifier, onTap: (VocabButton) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.chunked(5).take(4).forEach { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { button ->
                    VocabTile(button, Modifier.weight(1f).fillMaxHeight(), onTap)
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VocabTile(button: VocabButton, modifier: Modifier, onTap: (VocabButton) -> Unit) {
    Button(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(button.color)),
        onClick = { onTap(button) },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (button.isCategory || button.boardId != null) {
                Text("▣", modifier = Modifier.align(Alignment.TopEnd), color = Color(0xFF56616F), fontSize = 14.sp)
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(button.icon, fontSize = if (button.icon.length > 3) 18.sp else 31.sp, color = Color.Black, maxLines = 1)
                Text(button.label, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Button(
                onClick = { onTap(button) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(button.icon, fontSize = if (button.icon.length > 3) 13.sp else 20.sp, color = Color.Black, maxLines = 1)
                    Text(button.label, fontSize = 15.sp, color = Color.Black, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Admin", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(passcode, { passcode = it.filter(Char::isDigit).take(8) }, label = { Text("Passcode") })
                if (error) Text("Wrong passcode", color = Color(0xFFD53B3B))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("cancel") }
                    Button(onClick = {
                        if (passcode == Store.passcode(context)) onSuccess() else error = true
                    }) { Text("open") }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(
    homeButtons: List<VocabButton>,
    onButtonsChanged: (List<VocabButton>) -> Unit,
    onRestoreLayout: () -> Unit,
    onRestoreAll: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val usage = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(Unit) {
        usage.clear()
        Store.usageSummary(context).forEach { usage[it.first] = it.second }
    }

    FullScreenOverlay {
        Card(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.92f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Admin editor", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onRestoreLayout) { Text("restore layout") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onRestoreAll) { Text("restore all") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onClose) { Text("done") }
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LazyColumn(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(homeButtons) { index, item ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2F6FA)).padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("${index + 1}. ${item.icon} ${item.label}", Modifier.weight(1f), fontSize = 18.sp)
                                OutlinedButton(enabled = index > 0, onClick = {
                                    val next = homeButtons.toMutableList()
                                    val moved = next.removeAt(index)
                                    next.add(index - 1, moved)
                                    onButtonsChanged(next)
                                }) { Text("up") }
                                OutlinedButton(enabled = index < homeButtons.lastIndex, onClick = {
                                    val next = homeButtons.toMutableList()
                                    val moved = next.removeAt(index)
                                    next.add(index + 1, moved)
                                    onButtonsChanged(next)
                                }) { Text("down") }
                                Button(onClick = { editingIndex = index }) { Text("edit") }
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Local usage", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (usage.isEmpty()) Text("No words tracked yet.")
                        usage.forEach { (word, count) ->
                            Text("$word: $count", fontSize = 17.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = {
                            val next = homeButtons.toMutableList()
                            next.add(VocabButton("custom_${System.currentTimeMillis()}", "new", "new", "□", 0xFFFFFFFF))
                            onButtonsChanged(next.take(20))
                        }) { Text("add home word") }
                    }
                }
            }
        }
    }

    editingIndex?.let { index ->
        EditButtonDialog(
            item = homeButtons[index],
            onDismiss = { editingIndex = null },
            onSave = { updated ->
                val next = homeButtons.toMutableList()
                next[index] = updated
                onButtonsChanged(next)
                editingIndex = null
            },
        )
    }
}

@Composable
private fun EditButtonDialog(item: VocabButton, onDismiss: () -> Unit, onSave: (VocabButton) -> Unit) {
    var label by remember(item.id) { mutableStateOf(item.label) }
    var speech by remember(item.id) { mutableStateOf(item.speech) }
    var icon by remember(item.id) { mutableStateOf(item.icon) }

    FullScreenOverlay {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Edit word", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(label, { label = it.take(18) }, label = { Text("Label") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(speech, { speech = it.take(32) }, label = { Text("Speech") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Icon text") })
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("cancel") }
                    Button(onClick = {
                        onSave(item.copy(label = label.ifBlank { item.label }, speech = speech.ifBlank { label }, icon = icon.ifBlank { "□" }))
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
            .background(Color(0x99000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
