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
    val imagePath: String? = null,
    val addToSentence: Boolean = true,
    val grammarRole: GrammarRole = GrammarRole.Object,
)

private data class SentenceToken(
    val label: String,
    val speech: String,
    val icon: String,
    val imagePath: String?,
    val grammarRole: GrammarRole,
    val boardId: String?,
)

private enum class GrammarRole(val title: String, val help: String) {
    Subject("Subject", "I, you, Mum, Dad, teacher"),
    Intent("Intent", "want, need"),
    Negation("Negation", "don't, do not, not"),
    Action("Action", "go, help, play, wash, like"),
    Object("Object", "toy, book, blanket, general things"),
    FoodDrink("Food or drink", "water, apple, snack, cup"),
    Place("Place", "home, school, outside, shops"),
    Toilet("Toilet", "toilet, bathroom"),
    Feeling("Feeling", "happy, sad, sick, tired"),
    BodyPart("Body part", "head, hand, tummy, mouth"),
    Modifier("Modifier", "now, more"),
    Response("Response", "yes, no, finished, stop"),
    None("No grammar", "speak exactly as tapped"),
}

private data class VoiceOption(
    val name: String,
    val label: String,
)

private data class ChildProfile(
    val id: String,
    val name: String,
)

private data class UsageInsights(
    val uniqueWordsThisMonth: Int,
    val uniqueWordsPreviousMonth: Int,
    val uniqueTrend: Int,
    val topWords: List<Pair<String, Int>>,
    val topSentences: List<Pair<String, Int>>,
)

private object Store {
    private const val PREFS = "openaac"
    private const val KEY_SETUP = "setup_complete"
    private const val KEY_PASSCODE = "admin_passcode"
    private const val KEY_HOME = "home_buttons"
    private const val KEY_BOARDS = "board_layouts"
    private const val KEY_SENTENCES = "sentence_history"
    private const val KEY_COUNTS = "usage_counts"
    private const val KEY_WORD_EVENTS = "word_events"
    private const val KEY_TRANSITIONS = "transition_counts"
    private const val KEY_VOICE_NAME = "voice_name"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_GRAMMAR_CORRECTION = "grammar_correction"
    private const val KEY_PROFILES = "profiles"
    private const val KEY_CURRENT_PROFILE = "current_profile"
    private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    private const val SIXTY_DAYS_MS = 60L * 24L * 60L * 60L * 1000L
    private const val DEFAULT_PROFILE_ID = "default"

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

    fun profiles(context: Context): List<ChildProfile> {
        val raw = prefs(context).getString(KEY_PROFILES, null)
        val parsed = runCatching {
            if (raw.isNullOrBlank()) emptyList() else {
                val array = JSONArray(raw)
                List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    ChildProfile(
                        id = item.optString("id", DEFAULT_PROFILE_ID).ifBlank { DEFAULT_PROFILE_ID },
                        name = item.optString("name", "Child").ifBlank { "Child" },
                    )
                }
            }
        }.getOrDefault(emptyList())
        return parsed.ifEmpty { listOf(ChildProfile(DEFAULT_PROFILE_ID, "Child")) }
    }

    fun currentProfile(context: Context): ChildProfile {
        val all = profiles(context)
        val currentId = prefs(context).getString(KEY_CURRENT_PROFILE, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
        return all.firstOrNull { it.id == currentId } ?: all.first()
    }

    fun saveCurrentProfile(context: Context, profileId: String) {
        val safeId = profiles(context).firstOrNull { it.id == profileId }?.id ?: DEFAULT_PROFILE_ID
        prefs(context).edit().putString(KEY_CURRENT_PROFILE, safeId).apply()
    }

    fun addProfile(context: Context, name: String): ChildProfile {
        val cleanName = name.trim().take(24).ifBlank { "Child ${profiles(context).size + 1}" }
        val baseId = cleanName.normalizedId().ifBlank { "profile" }
        val existing = profiles(context).map { it.id }.toSet()
        var nextId = baseId
        var suffix = 2
        while (nextId in existing) {
            nextId = "${baseId}_$suffix"
            suffix++
        }
        val profile = ChildProfile(nextId, cleanName)
        saveProfiles(context, profiles(context) + profile)
        saveCurrentProfile(context, profile.id)
        return profile
    }

    private fun saveProfiles(context: Context, profiles: List<ChildProfile>) {
        val array = JSONArray()
        profiles.distinctBy { it.id }.forEach { profile ->
            array.put(JSONObject().put("id", profile.id).put("name", profile.name))
        }
        prefs(context).edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    fun voiceName(context: Context): String? =
        prefs(context).getString(profileKey(context, KEY_VOICE_NAME), null)

    fun saveVoiceName(context: Context, voiceName: String) {
        prefs(context).edit().putString(profileKey(context, KEY_VOICE_NAME), voiceName).apply()
    }

    fun speechRate(context: Context): Float =
        prefs(context).getFloat(profileKey(context, KEY_SPEECH_RATE), 1.0f)

    fun saveSpeechRate(context: Context, rate: Float) {
        prefs(context).edit().putFloat(profileKey(context, KEY_SPEECH_RATE), rate.coerceIn(0.6f, 1.4f)).apply()
    }

    fun grammarCorrectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(profileKey(context, KEY_GRAMMAR_CORRECTION), true)

    fun saveGrammarCorrectionEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(profileKey(context, KEY_GRAMMAR_CORRECTION), enabled).apply()
    }

    fun homeButtons(context: Context): List<VocabButton> {
        val raw = prefs(context).getString(profileKey(context, KEY_HOME), null)
            ?: prefs(context).getString(KEY_HOME, null)
            ?: return Defaults.home
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toButton() }
        }.getOrDefault(Defaults.home)
    }

    fun saveHomeButtons(context: Context, buttons: List<VocabButton>) {
        val array = JSONArray()
        buttons.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString(profileKey(context, KEY_HOME), array.toString()).apply()
    }

    fun boards(context: Context): Map<String, List<VocabButton>> {
        val fallback = Defaults.allBoards()
        val raw = prefs(context).getString(profileKey(context, KEY_BOARDS), null)
            ?: prefs(context).getString(KEY_BOARDS, null)?.takeIf { currentProfile(context).id == DEFAULT_PROFILE_ID }
        if (raw.isNullOrBlank()) {
            val legacyHome = prefs(context).getString(profileKey(context, KEY_HOME), null)
                ?: prefs(context).getString(KEY_HOME, null)?.takeIf { currentProfile(context).id == DEFAULT_PROFILE_ID }
            val parsedLegacyHome = legacyHome?.let {
                runCatching {
                    val array = JSONArray(it)
                    List(array.length()) { index -> array.getJSONObject(index).toButton() }
                }.getOrNull()
            }
            return if (parsedLegacyHome == null) fallback else fallback + (Defaults.HOME_BOARD to parsedLegacyHome)
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
            .putString(profileKey(context, KEY_BOARDS), root.toString())
            .putString(profileKey(context, KEY_HOME), JSONArray().also { array ->
                boards[Defaults.HOME_BOARD].orEmpty().forEach { array.put(it.toJson()) }
            }.toString())
            .apply()
    }

    fun restoreDefaultLayout(context: Context) {
        prefs(context).edit()
            .remove(profileKey(context, KEY_HOME))
            .remove(profileKey(context, KEY_BOARDS))
            .apply()
    }

    fun wipeLearnedHistory(context: Context) {
        prefs(context).edit()
            .remove(profileKey(context, KEY_SENTENCES))
            .remove(profileKey(context, KEY_COUNTS))
            .remove(profileKey(context, KEY_WORD_EVENTS))
            .remove(profileKey(context, KEY_TRANSITIONS))
            .apply()
    }

    fun restoreAllDefaults(context: Context) {
        prefs(context).edit()
            .remove(profileKey(context, KEY_HOME))
            .remove(profileKey(context, KEY_BOARDS))
            .remove(profileKey(context, KEY_SENTENCES))
            .remove(profileKey(context, KEY_COUNTS))
            .remove(profileKey(context, KEY_WORD_EVENTS))
            .remove(profileKey(context, KEY_TRANSITIONS))
            .remove(profileKey(context, KEY_VOICE_NAME))
            .remove(profileKey(context, KEY_SPEECH_RATE))
            .remove(profileKey(context, KEY_GRAMMAR_CORRECTION))
            .apply()
    }

    fun trackWord(context: Context, label: String) {
        val counts = JSONObject(prefs(context).getString(profileKey(context, KEY_COUNTS), "{}") ?: "{}")
        counts.put(label, counts.optInt(label, 0) + 1)
        val now = System.currentTimeMillis()
        val events = JSONArray(prefs(context).getString(profileKey(context, KEY_WORD_EVENTS), "[]") ?: "[]")
        val fresh = JSONArray()
        for (index in 0 until events.length()) {
            val item = events.getJSONObject(index)
            if (now - item.optLong("at", 0L) <= SIXTY_DAYS_MS) fresh.put(item)
        }
        fresh.put(JSONObject().put("at", now).put("word", label))
        prefs(context).edit()
            .putString(profileKey(context, KEY_COUNTS), counts.toString())
            .putString(profileKey(context, KEY_WORD_EVENTS), fresh.toString())
            .apply()
    }

    fun trackTransition(context: Context, previous: String?, next: String) {
        if (previous.isNullOrBlank() || next.isBlank()) return
        val transitions = JSONObject(prefs(context).getString(profileKey(context, KEY_TRANSITIONS), "{}") ?: "{}")
        val key = transitionKey(previous, next)
        transitions.put(key, transitions.optInt(key, 0) + 1)
        prefs(context).edit().putString(profileKey(context, KEY_TRANSITIONS), transitions.toString()).apply()
    }

    fun trackSentence(context: Context, spoken: String) {
        if (spoken.isBlank()) return
        val now = System.currentTimeMillis()
        val source = JSONArray(prefs(context).getString(profileKey(context, KEY_SENTENCES), "[]") ?: "[]")
        val fresh = JSONArray()
        for (index in 0 until source.length()) {
            val item = source.getJSONObject(index)
            if (now - item.optLong("at", 0L) <= THIRTY_DAYS_MS) fresh.put(item)
        }
        fresh.put(JSONObject().put("at", now).put("spoken", spoken))
        prefs(context).edit().putString(profileKey(context, KEY_SENTENCES), fresh.toString()).apply()
    }

    fun usageInsights(context: Context): UsageInsights {
        val counts = JSONObject(prefs(context).getString(profileKey(context, KEY_COUNTS), "{}") ?: "{}")
        val now = System.currentTimeMillis()
        val currentStart = now - THIRTY_DAYS_MS
        val previousStart = now - SIXTY_DAYS_MS
        val currentWords = mutableSetOf<String>()
        val previousWords = mutableSetOf<String>()
        val events = JSONArray(prefs(context).getString(profileKey(context, KEY_WORD_EVENTS), "[]") ?: "[]")
        for (index in 0 until events.length()) {
            val item = events.getJSONObject(index)
            val at = item.optLong("at", 0L)
            val word = item.optString("word").trim()
            if (word.isBlank()) continue
            when {
                at >= currentStart -> currentWords.add(word.normalized())
                at >= previousStart -> previousWords.add(word.normalized())
            }
        }

        val sentenceCounts = linkedMapOf<String, Int>()
        val sentences = JSONArray(prefs(context).getString(profileKey(context, KEY_SENTENCES), "[]") ?: "[]")
        for (index in 0 until sentences.length()) {
            val spoken = sentences.getJSONObject(index).optString("spoken").trim()
            if (spoken.isNotBlank()) sentenceCounts[spoken] = (sentenceCounts[spoken] ?: 0) + 1
        }

        val topWords = counts.keys().asSequence()
            .map { it to counts.optInt(it) }
            .filter { it.first.isNotBlank() && it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)
            .toList()
        val topSentences = sentenceCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }
        return UsageInsights(
            uniqueWordsThisMonth = currentWords.size,
            uniqueWordsPreviousMonth = previousWords.size,
            uniqueTrend = currentWords.size - previousWords.size,
            topWords = topWords,
            topSentences = topSentences,
        )
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

        val counts = JSONObject(prefs(context).getString(profileKey(context, KEY_COUNTS), "{}") ?: "{}")
        val transitions = JSONObject(prefs(context).getString(profileKey(context, KEY_TRANSITIONS), "{}") ?: "{}")
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

    private fun profileKey(context: Context, key: String): String =
        "${currentProfile(context).id}_$key"

    fun copyImageToPrivateStorage(context: Context, uri: Uri): String? {
        return runCatching {
            val profileId = currentProfile(context).id
            val dir = File(context.filesDir, "profiles/$profileId/images").apply { mkdirs() }
            val target = File(dir, "image_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            target.absolutePath
        }.getOrNull()
    }

    fun applySharedVisuals(boards: Map<String, List<VocabButton>>, source: VocabButton): Map<String, List<VocabButton>> {
        val targetLabel = source.label.normalized()
        if (targetLabel.isBlank()) return boards
        return boards.mapValues { (_, buttons) ->
            buttons.map { button ->
                if (button.label.normalized() == targetLabel) {
                    button.copy(
                        icon = source.icon,
                        imagePath = source.imagePath,
                        addToSentence = if (source.boardId != null) source.addToSentence else button.addToSentence,
                        grammarRole = source.grammarRole,
                    )
                } else {
                    button
                }
            }
        }
    }

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
        .put("imagePath", imagePath)
        .put("addToSentence", addToSentence)
        .put("grammarRole", grammarRole.name)

    private fun JSONObject.toButton() = VocabButton(
        id = optString("id"),
        label = optString("label"),
        speech = optString("speech", optString("label")),
        icon = optString("icon", "□"),
        color = optLong("color", 0xFFFFFFFF),
        boardId = optString("boardId").ifBlank { null },
        isCategory = optBoolean("isCategory", false),
        imagePath = optString("imagePath").ifBlank { null },
        addToSentence = optBoolean("addToSentence", true),
        grammarRole = optString("grammarRole")
            .takeIf { it.isNotBlank() }
            ?.let { raw -> GrammarRole.entries.firstOrNull { it.name == raw } }
            ?: inferGrammarRole(
                label = optString("label"),
                speech = optString("speech", optString("label")),
                boardId = optString("boardId").ifBlank { null },
            ),
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

private fun inferGrammarRole(label: String, speech: String = label, boardId: String? = null): GrammarRole {
    val text = speech.ifBlank { label }.normalizedGrammarText()
    val board = boardId?.normalizedGrammarText()
    return when {
        text in setOf("i", "you", "me", "mum", "dad", "friend", "teacher") -> GrammarRole.Subject
        text in setOf("want", "need") -> GrammarRole.Intent
        text in setOf("don't", "dont", "do not", "not") -> GrammarRole.Negation
        text in setOf("go", "help", "play", "wash", "rest", "like", "have", "drink", "eat", "hurts", "hurt") -> GrammarRole.Action
        text in setOf("home", "school", "outside", "shops") || board == "places" -> GrammarRole.Place
        text in setOf("toilet", "bathroom") || board == "toilet" -> GrammarRole.Toilet
        text in setOf("food", "drink", "water", "juice", "milk", "cup", "apple", "banana", "bread", "snack") ||
            board in setOf("food", "drink") -> GrammarRole.FoodDrink
        text in setOf("happy", "sad", "sick", "tired", "angry") || board == "feel" -> GrammarRole.Feeling
        text in setOf("head", "hand", "mouth", "tummy") || board == "body" -> GrammarRole.BodyPart
        text in setOf("more", "now") -> GrammarRole.Modifier
        text in setOf("yes", "no", "stop", "finished") -> GrammarRole.Response
        board != null -> GrammarRole.Object
        else -> GrammarRole.Object
    }
}

private fun String.normalizedGrammarText(): String =
    lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")

private object GrammarEngine {
    fun realize(tokens: List<SentenceToken>, enabled: Boolean): String {
        val raw = tokens.joinToString(" ") { it.speech }.trim()
        if (!enabled || tokens.isEmpty()) return raw

        val usable = tokens.filter { it.grammarRole != GrammarRole.None && it.speech.isNotBlank() }
        if (usable.isEmpty()) return raw

        val words = usable.map { it.speech.trim() }
        val normalized = words.map { it.normalizedGrammarText() }
        if (normalized.size == 1) return singleWord(usable.first())

        val sentences = splitClauses(usable)
            .flatMap { renderClause(it) }
            .filter { it.isNotBlank() }
        if (sentences.isNotEmpty()) return sentences.joinToString(" ")

        return cleanup(raw)
    }

    private fun splitClauses(tokens: List<SentenceToken>): List<List<SentenceToken>> {
        val clauses = mutableListOf<List<SentenceToken>>()
        val current = mutableListOf<SentenceToken>()

        tokens.forEach { token ->
            val hasContent = current.any { it.grammarRole != GrammarRole.Subject }
            val startsNewSubject = token.grammarRole == GrammarRole.Subject && hasContent
            val startsNewIntent = token.grammarRole == GrammarRole.Intent &&
                current.any { it.grammarRole in setOf(GrammarRole.Feeling, GrammarRole.Toilet, GrammarRole.Place, GrammarRole.Response) }
            if (current.isNotEmpty() && (startsNewSubject || startsNewIntent)) {
                clauses.add(current.toList())
                current.clear()
            }
            current.add(token)
        }
        if (current.isNotEmpty()) clauses.add(current.toList())
        return clauses
    }

    private fun renderClause(tokens: List<SentenceToken>): List<String> {
        negationSentence(tokens)?.let { return listOf(it) }
        painSentence(tokens)?.let { return listOf(it) }
        bodySymptomSentence(tokens)?.let { return listOf(it) }

        val primary = toiletSentence(tokens)
            ?: placeSentence(tokens)
            ?: actionRequestSentence(tokens)
            ?: requestSentence(tokens)
            ?: helpSentence(tokens)
        if (primary != null) {
            return listOfNotNull(primary, feelingSentence(tokens.takeLastWhile { it.grammarRole == GrammarRole.Feeling }))
        }

        feelingSentence(tokens)?.let { return listOf(it) }
        return listOf(cleanup(tokens.joinToString(" ") { it.speech }))
    }

    private fun singleWord(token: SentenceToken): String {
        val text = token.speech.trim()
        return when (token.grammarRole) {
            GrammarRole.Toilet -> "toilet"
            else -> text
        }
    }

    private fun painSentence(tokens: List<SentenceToken>): String? {
        val bodyPart = tokens.firstOrNull { it.grammarRole == GrammarRole.BodyPart }?.speech?.trim() ?: return null
        val hasPain = tokens.any { it.speech.normalizedGrammarText() in setOf("hurt", "hurts", "sore", "pain") }
        if (!hasPain) return null
        val owner = if (tokens.firstOrNull { it.grammarRole == GrammarRole.Subject }?.speech?.normalizedGrammarText() == "you") "Your" else "My"
        return "$owner $bodyPart hurts."
    }

    private fun bodySymptomSentence(tokens: List<SentenceToken>): String? {
        val bodyPart = tokens.firstOrNull { it.grammarRole == GrammarRole.BodyPart }?.speech?.trim() ?: return null
        val symptom = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Feeling &&
                it.speech.normalizedGrammarText() in setOf("sick", "tired", "sore")
        }?.speech?.trim() ?: return null
        val owner = if (tokens.firstOrNull { it.grammarRole == GrammarRole.Subject }?.speech?.normalizedGrammarText() == "you") "Your" else "My"
        return "$owner $bodyPart feels $symptom."
    }

    private fun feelingSentence(tokens: List<SentenceToken>): String? {
        if (tokens.any { it.isNegation() }) return null
        val feelings = tokens
            .filter { it.grammarRole == GrammarRole.Feeling }
            .map { it.speech.trim() }
            .filter { it.isNotBlank() && it.normalizedGrammarText() != "feel" }
            .distinctBy { it.normalizedGrammarText() }
        if (feelings.isEmpty()) return null
        val subject = subjectText(tokens) ?: "I"
        return "$subject ${verbForSubject(subject, "feel", "feels")} ${feelings.joinForSpeech()}."
    }

    private fun negationSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.isNegation() }) return null
        val subject = subjectText(tokens) ?: "I"
        val negation = negationForSubject(subject)
        val action = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action &&
                it.speech.normalizedGrammarText() !in setOf("hurt", "hurts")
        }?.speech?.normalizedGrammarText()
        val intent = intentText(tokens)

        val toilet = tokens.firstOrNull { it.grammarRole == GrammarRole.Toilet }?.speech?.trim()
        val place = tokens.firstOrNull { it.grammarRole == GrammarRole.Place }?.speech?.trim()
        val feeling = tokens
            .filter { it.grammarRole == GrammarRole.Feeling }
            .map { it.speech.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.normalizedGrammarText() }
            .joinForSpeech()
        val target = tokens.firstOrNull {
            it.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) &&
                it.speech.normalizedGrammarText() !in setOf("food", "drink", "things")
        }?.speech?.trim()

        return when {
            action == "feel" && feeling.isNotBlank() -> "$subject $negation feel $feeling."
            action == "like" && place != null -> "$subject $negation like $place."
            action == "like" && toilet != null -> "$subject $negation like ${toiletArticle(toilet)}$toilet."
            action == "like" && target != null -> "$subject $negation like $target."
            action == "go" && place != null -> "$subject $negation want to go $place."
            action == "go" && toilet != null -> "$subject $negation want to go to ${toiletArticle(toilet)}$toilet."
            intent != null && toilet != null -> "$subject $negation $intent to go to ${toiletArticle(toilet)}$toilet."
            intent != null && place != null -> "$subject $negation $intent to go $place."
            intent != null && target != null -> "$subject $negation $intent $target."
            feeling.isNotBlank() -> "$subject $negation feel $feeling."
            target != null -> "$subject $negation want $target."
            else -> cleanup(tokens.joinToString(" ") { it.speech })
        }
    }

    private fun toiletSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.grammarRole == GrammarRole.Toilet }) return null
        val subject = subjectText(tokens) ?: "I"
        val intent = intentText(tokens) ?: "want"
        val toilet = tokens.firstOrNull { it.grammarRole == GrammarRole.Toilet }?.speech?.trim().orEmpty()
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} to go to ${toiletArticle(toilet)}$toilet."
    }

    private fun placeSentence(tokens: List<SentenceToken>): String? {
        val place = tokens.firstOrNull { it.grammarRole == GrammarRole.Place }?.speech?.trim() ?: return null
        val hasGo = tokens.any { it.speech.normalizedGrammarText() == "go" || it.boardId?.normalizedGrammarText() == "go" }
        if (!hasGo && tokens.none { it.grammarRole == GrammarRole.Intent }) return null
        val subject = subjectText(tokens) ?: "I"
        val intent = intentText(tokens) ?: "want"
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} to go $place."
    }

    private fun requestSentence(tokens: List<SentenceToken>): String? {
        val intent = intentText(tokens) ?: return null
        val subject = subjectText(tokens) ?: "I"
        val target = tokens.firstOrNull {
            it.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) &&
                it.speech.normalizedGrammarText() !in setOf("food", "drink", "things")
        }?.speech?.trim() ?: return null
        val determiner = if (tokens.firstOrNull { it.grammarRole == GrammarRole.FoodDrink } != null) "some " else ""
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} $determiner$target."
    }

    private fun actionRequestSentence(tokens: List<SentenceToken>): String? {
        val intent = intentText(tokens) ?: return null
        val subject = subjectText(tokens) ?: "I"
        val action = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action &&
                it.speech.normalizedGrammarText() !in setOf("feel", "go", "help", "hurt", "hurts")
        }?.speech?.trim() ?: return null
        val target = tokens.firstOrNull {
            it.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) &&
                it.speech.normalizedGrammarText() !in setOf("food", "drink", "things")
        }?.speech?.trim()
        val suffix = target?.let { " $it" }.orEmpty()
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} to $action$suffix."
    }

    private fun helpSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.speech.normalizedGrammarText() == "help" }) return null
        val subject = subjectText(tokens)
        return if (subject?.normalizedGrammarText() == "you") "Can you help me?" else "I need help."
    }

    private fun subjectText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull { it.grammarRole == GrammarRole.Subject }?.speech?.trim()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

    private fun intentText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull { it.grammarRole == GrammarRole.Intent }?.speech?.normalizedGrammarText()

    private fun verbForSubject(subject: String, firstPerson: String, thirdPerson: String): String =
        if (subject.normalizedGrammarText() in setOf("i", "you")) firstPerson else thirdPerson

    private fun negationForSubject(subject: String): String =
        if (subject.normalizedGrammarText() in setOf("i", "you")) "don't" else "doesn't"

    private fun toiletArticle(toilet: String): String =
        if (toilet.normalizedGrammarText() in setOf("toilet", "bathroom")) "the " else ""

    private fun SentenceToken.isNegation(): Boolean =
        grammarRole == GrammarRole.Negation || speech.normalizedGrammarText() in setOf("don't", "dont", "do not", "not")

    private fun cleanup(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        return if (trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith("!")) trimmed else "$trimmed."
    }

    private fun List<String>.joinForSpeech(): String =
        when (size) {
            0 -> ""
            1 -> first()
            2 -> "${first()} and ${last()}"
            else -> dropLast(1).joinToString(", ") + ", and " + last()
        }
}

private object Defaults {
    const val HOME_BOARD = "home"

    val pinned = listOf(
        VocabButton("pin_yes", "yes", "yes", "✓", 0xFFA8E6A1, grammarRole = GrammarRole.Response),
        VocabButton("pin_no", "no", "no", "✕", 0xFFFFB3A7, grammarRole = GrammarRole.Response),
        VocabButton("pin_more", "more", "more", "+", 0xFFFFE08A, grammarRole = GrammarRole.Modifier),
        VocabButton("pin_help", "help", "help", "?", 0xFFFFC36E, grammarRole = GrammarRole.Action),
        VocabButton("pin_stop", "stop", "stop", "STOP", 0xFFFF6B6B, grammarRole = GrammarRole.Response),
    )

    val home = listOf(
        VocabButton("i", "I", "I", "☝", 0xFFFFFFFF, "i", true, grammarRole = GrammarRole.Subject),
        VocabButton("you", "you", "you", "👤", 0xFFFFFFFF, "you", true, grammarRole = GrammarRole.Subject),
        VocabButton("want", "want", "want", "★", 0xFFFFF3A3, "want", true, grammarRole = GrammarRole.Intent),
        VocabButton("need", "need", "need", "!", 0xFFFFD1A8, "need", true, grammarRole = GrammarRole.Intent),
        VocabButton("go", "go", "go", "→", 0xFFB8F5C7, "go", true, grammarRole = GrammarRole.Action),
        VocabButton("food", "food", "food", "🍎", 0xFFFFDFA6, "food", true, grammarRole = GrammarRole.FoodDrink),
        VocabButton("drink", "drink", "drink", "🥤", 0xFFAEE8FF, "drink", true, grammarRole = GrammarRole.FoodDrink),
        VocabButton("toilet", "toilet", "toilet", "🚽", 0xFFD8E6FF, "toilet", true, grammarRole = GrammarRole.Toilet),
        VocabButton("people", "people", "people", "●●", 0xFFFFC98F, "people", true, grammarRole = GrammarRole.Subject),
        VocabButton("places", "places", "places", "⌂", 0xFFBDE8C9, "places", true, grammarRole = GrammarRole.Place),
        VocabButton("home", "home", "home", "⌂", 0xFFBDE8C9, grammarRole = GrammarRole.Place),
        VocabButton("school", "school", "school", "▣", 0xFFCDEB91, grammarRole = GrammarRole.Place),
        VocabButton("play", "play", "play", "▶", 0xFFD7B5FF, "play", true, grammarRole = GrammarRole.Action),
        VocabButton("feel", "feel", "feel", "☺", 0xFFFFC6E3, "feel", true, grammarRole = GrammarRole.Action),
        VocabButton("body", "body", "body", "✋", 0xFFAEE8FF, "body", true, grammarRole = GrammarRole.BodyPart),
        VocabButton("like", "like", "like", "♡", 0xFFFFF3A3, grammarRole = GrammarRole.Action),
        VocabButton("dont", "don't", "don't", "✕", 0xFFFFB3A7, grammarRole = GrammarRole.Negation),
        VocabButton("have", "have", "have", "▣", 0xFFFFFFFF, grammarRole = GrammarRole.Action),
        VocabButton("things", "things", "things", "□", 0xFFE0E0E0, "things", true, grammarRole = GrammarRole.Object),
        VocabButton("finished", "finished", "finished", "✓", 0xFFCFCFCF, grammarRole = GrammarRole.Response),
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
        color: Long = 0xFFFFFFFF,
    ) = VocabButton(
        id = label.lowercase(Locale.ROOT).replace(" ", "_"),
        label = label,
        speech = label,
        icon = icon,
        color = color,
        boardId = boardId,
        isCategory = boardId != null,
        grammarRole = inferGrammarRole(label = label, boardId = boardId),
    )

    private fun category(label: String, icon: String = "□") =
        word(label, icon, label, colorForFolder(label))

    fun allBoards(): Map<String, List<VocabButton>> = boards + (HOME_BOARD to home)

    private fun colorForFolder(label: String): Long =
        ButtonPalette.colors.firstOrNull { it.label.equals(label, ignoreCase = true) }?.value
            ?: ButtonPalette.colors[(kotlin.math.abs(label.hashCode()) % ButtonPalette.colors.size)].value
}

private data class FolderColorOption(
    val label: String,
    val value: Long,
)

private object ButtonPalette {
    val colors = listOf(
        FolderColorOption("want", 0xFFFFF3A3),
        FolderColorOption("need", 0xFFFFD1A8),
        FolderColorOption("go", 0xFFB8F5C7),
        FolderColorOption("food", 0xFFFFDFA6),
        FolderColorOption("drink", 0xFFAEE8FF),
        FolderColorOption("toilet", 0xFFD8E6FF),
        FolderColorOption("people", 0xFFFFC98F),
        FolderColorOption("places", 0xFFBDE8C9),
        FolderColorOption("play", 0xFFD7B5FF),
        FolderColorOption("feel", 0xFFFFC6E3),
        FolderColorOption("body", 0xFFA9D6C9),
        FolderColorOption("things", 0xFFE0E0E0),
        FolderColorOption("plain", 0xFFFFFFFF),
    )
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
    speakWord: (String) -> Unit,
    speakSentence: (String) -> Unit,
    stopSpeech: () -> Unit,
    sentenceSpeaking: Boolean,
    currentProfile: ChildProfile,
    profiles: List<ChildProfile>,
    onProfileSelected: (String) -> Unit,
    onAddProfile: (String) -> Unit,
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
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var recommendationRefresh by remember { mutableStateOf(0) }
    var usageRefresh by remember { mutableStateOf(0) }
    var grammarCorrectionEnabled by remember { mutableStateOf(Store.grammarCorrectionEnabled(context)) }

    LaunchedEffect(currentProfile.id) {
        boardsById = Store.boards(context)
        draftBoards = boardsById
        sentence.clear()
        boardStack.clear()
        reorganizing = false
        showAddButton = false
        editingButtonIndex = null
        pendingDeleteIndex = null
        grammarCorrectionEnabled = Store.grammarCorrectionEnabled(context)
        recommendationRefresh++
        usageRefresh++
    }

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
        if (button.addToSentence) {
            val previous = sentence.lastOrNull()?.label
            sentence.add(SentenceToken(button.label, button.speech, button.icon, button.imagePath, button.grammarRole, button.boardId))
            speakWord(button.speech)
            Store.trackWord(context, button.label)
            Store.trackTransition(context, previous, button.label)
            recommendationRefresh++
        }
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
        draftBoards = Store.applySharedVisuals(nextBoards, button)
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
                    speaking = sentenceSpeaking,
                    onSpeak = {
                        val spoken = GrammarEngine.realize(sentence, grammarCorrectionEnabled)
                        speakSentence(spoken)
                        Store.trackSentence(context, spoken)
                    },
                    onStop = stopSpeech,
                    onBackspace = { if (sentence.isNotEmpty()) sentence.removeAt(sentence.lastIndex) },
                    onClear = { sentence.clear() },
                    onRemoveAt = { index ->
                        if (index in sentence.indices) sentence.removeAt(index)
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
                    onDelete = { pendingDeleteIndex = it },
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
                grammarCorrectionEnabled = true
                voiceOptions.firstOrNull()?.let { onVoiceSelected(it.name) }
            },
            onWipeLearning = {
                Store.wipeLearnedHistory(context)
                recommendationRefresh++
                usageRefresh++
            },
            currentProfile = currentProfile,
            profiles = profiles,
            onProfileSelected = onProfileSelected,
            onAddProfile = onAddProfile,
            voiceOptions = voiceOptions,
            selectedVoiceName = selectedVoiceName,
            speechRate = speechRate,
            onVoiceSelected = onVoiceSelected,
            onSpeechRateChanged = onSpeechRateChanged,
            grammarCorrectionEnabled = grammarCorrectionEnabled,
            onGrammarCorrectionChanged = {
                grammarCorrectionEnabled = it
                Store.saveGrammarCorrectionEnabled(context, it)
            },
            onTestVoice = { speakWord("I want food") },
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
                        draftBoards = Store.applySharedVisuals(nextBoards, updated)
                    }
                    editingButtonIndex = null
                },
            )
        } ?: run {
            editingButtonIndex = null
        }
    }

    pendingDeleteIndex?.let { index ->
        draftBoards[currentBoardId]?.getOrNull(index)?.let { item ->
            ConfirmDeleteDialog(
                label = item.label,
                onDismiss = { pendingDeleteIndex = null },
                onConfirm = {
                    val nextBoards = draftBoards.toMutableMap()
                    nextBoards[currentBoardId] = nextBoards[currentBoardId].orEmpty()
                        .filterIndexed { buttonIndex, _ -> buttonIndex != index }
                    draftBoards = nextBoards
                    pendingDeleteIndex = null
                },
            )
        } ?: run {
            pendingDeleteIndex = null
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
    speaking: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onRemoveAt: (Int) -> Unit,
    onAdmin: () -> Unit,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(sentence.size) {
        if (selectedIndex != null && selectedIndex !in sentence.indices) selectedIndex = null
    }
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
        ActionButton(
            label = if (speaking) "stop" else "speak",
            icon = if (speaking) "■" else "▶",
            color = if (speaking) Color(0xFFE05555) else Color(0xFF2166F3),
            onClick = if (speaking) onStop else onSpeak,
        )
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sentence.forEachIndexed { index, token ->
                SentenceChip(
                    token = token,
                    selected = selectedIndex == index,
                    onTap = {
                        if (selectedIndex == index) {
                            onRemoveAt(index)
                            selectedIndex = null
                        } else {
                            selectedIndex = index
                        }
                    },
                )
            }
        }
        BackspaceButton(onBackspace = onBackspace)
        ActionButton("clear", "×", Color(0xFFE05555), onClear)
        OutlinedButton(onClick = onAdmin, modifier = Modifier.height(70.dp)) {
            Text("admin", fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SentenceChip(token: SentenceToken, selected: Boolean, onTap: () -> Unit) {
    Box(
        Modifier
            .width(82.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFFE6E6) else Color(0xFFF2F6FA))
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) Color(0xFFE05555) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .combinedClickable(onClick = onTap),
    ) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ButtonVisual(icon = token.icon, imagePath = token.imagePath, modifier = Modifier.size(36.dp), iconFontSize = 24)
            Text(token.label, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(RoundedCornerShape(bottomStart = 6.dp))
                    .background(Color(0xFFE05555)),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
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
    onDelete: (Int) -> Unit = {},
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
                        onDelete = onDelete,
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
    onDelete: (Int) -> Unit,
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
        onDelete = if (reorganizing) ({ onDelete(index) }) else null,
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
    onDelete: (() -> Unit)? = null,
    dropPreview: DropAction? = null,
) {
    val isFolder = button.isCategory || button.boardId != null
    val shape = if (isFolder) FolderShape() else RoundedCornerShape(12.dp)
    val borderColor = when (dropPreview) {
        DropAction.MoveIntoFolder -> Color(0xFF1F9D55)
        DropAction.MoveBefore,
        DropAction.MoveAfter -> Color(0xFF2166F3)
        null -> Color(button.color)
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
            ButtonVisual(
                icon = button.icon,
                imagePath = button.imagePath,
                modifier = Modifier.size(54.dp),
                iconFontSize = if (button.icon.length > 3) 18 else 28,
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
        onDelete?.let { delete ->
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE05555))
                    .combinedClickable(onClick = delete),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
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
            val borderColor = Color(button.color)
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
                    ButtonVisual(
                        icon = button.icon,
                        imagePath = button.imagePath,
                        modifier = Modifier.size(34.dp),
                        iconFontSize = if (button.icon.length > 3) 13 else 18,
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

@Composable
private fun ButtonVisual(icon: String, imagePath: String?, modifier: Modifier, iconFontSize: Int) {
    val bitmap = remember(imagePath) {
        imagePath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Text(
            text = icon,
            fontSize = iconFontSize.sp,
            color = Color.Black,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(onBackspace: () -> Unit) {
    Box(
        Modifier
            .width(78.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF5D6673))
            .combinedClickable(onClick = onBackspace),
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
    currentProfile: ChildProfile,
    profiles: List<ChildProfile>,
    onProfileSelected: (String) -> Unit,
    onAddProfile: (String) -> Unit,
    voiceOptions: List<VoiceOption>,
    selectedVoiceName: String?,
    speechRate: Float,
    onVoiceSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    grammarCorrectionEnabled: Boolean,
    onGrammarCorrectionChanged: (Boolean) -> Unit,
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
                            voiceOptions = voiceOptions,
                            selectedVoiceName = selectedVoiceName,
                            speechRate = speechRate,
                            onVoiceSelected = onVoiceSelected,
                            onSpeechRateChanged = onSpeechRateChanged,
                            onTestVoice = onTestVoice,
                        )
                        GrammarControls(
                            enabled = grammarCorrectionEnabled,
                            onToggle = { onGrammarCorrectionChanged(!grammarCorrectionEnabled) },
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
private fun ProfileSelector(
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
private fun UsageDashboard(insights: UsageInsights) {
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
private fun InsightList(title: String, rows: List<Pair<String, Int>>, emptyText: String) {
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
private fun AddProfileDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
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
private fun PasscodeResetDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
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
private fun ConfirmDeleteDialog(label: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F6FA))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Voice", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onTestVoice) { Text("test") }
                Text("Speed ${(speechRate * 100).toInt()}%", fontSize = 14.sp, color = Color(0xFF56616F))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSpeechRateChanged(speechRate - 0.1f) }) { Text("slower") }
                OutlinedButton(onClick = { onSpeechRateChanged(speechRate + 0.1f) }) { Text("faster") }
            }
        }
    }
}

@Composable
private fun GrammarControls(enabled: Boolean, onToggle: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7F1))) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Grammar correction", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (enabled) "Speak sentence uses local fixed grammar templates." else "Speak sentence uses tapped words exactly.",
                    fontSize = 13.sp,
                    color = Color(0xFF56616F),
                )
            }
            OutlinedButton(onClick = onToggle) {
                Text(if (enabled) "on" else "off")
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
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var speech by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("□") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var createFolder by remember { mutableStateOf(false) }
    var buttonColor by remember { mutableStateOf(ButtonPalette.colors.last().value) }
    var folderAddsToSentence by remember { mutableStateOf(true) }
    var grammarRole by remember { mutableStateOf(GrammarRole.Object) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imagePath = Store.copyImageToPrivateStorage(context, uri)
    }
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
                                ButtonVisual(button.icon, button.imagePath, Modifier.size(40.dp), 24)
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
                GrammarRoleDropdown(
                    selected = grammarRole,
                    onSelected = { grammarRole = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Icon text") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ButtonVisual(icon.ifBlank { "□" }, imagePath, Modifier.size(54.dp), 28)
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("choose image") }
                    if (imagePath != null) {
                        OutlinedButton(onClick = { imagePath = null }) { Text("remove") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { createFolder = !createFolder },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (createFolder) Color(0xFFFFF7DA) else Color.Transparent),
                    ) {
                        Text(if (createFolder) "folder on" else "make folder")
                    }
                    Text("Folders open a new board and can receive dropped buttons.", fontSize = 13.sp, color = Color(0xFF56616F))
                }
                ButtonColorPicker(selected = buttonColor, onSelected = { buttonColor = it })
                if (createFolder) {
                    FolderSpeakToggle(
                        enabled = folderAddsToSentence,
                        onToggle = { folderAddsToSentence = !folderAddsToSentence },
                    )
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
                                    color = buttonColor,
                                    boardId = boardId,
                                    isCategory = boardId != null,
                                    imagePath = imagePath,
                                    addToSentence = if (boardId != null) folderAddsToSentence else true,
                                    grammarRole = grammarRole,
                                )
                            )
                        },
                    ) { Text("create") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrammarRoleDropdown(
    selected: GrammarRole,
    onSelected: (GrammarRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Grammar role", fontSize = 14.sp, color = Color(0xFF56616F))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                GrammarRole.entries.forEach { role ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(role.title, fontSize = 15.sp)
                                Text(role.help, fontSize = 12.sp, color = Color(0xFF56616F), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelected(role)
                        },
                    )
                }
            }
        }
        Text(selected.help, fontSize = 12.sp, color = Color(0xFF56616F))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ButtonColorPicker(selected: Long, onSelected: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Button color", fontSize = 14.sp, color = Color(0xFF56616F))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ButtonPalette.colors.forEach { option ->
                val selectedColor = option.value == selected
                Box(
                    Modifier
                        .size(if (selectedColor) 34.dp else 30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(option.value))
                        .border(
                            width = if (selectedColor) 3.dp else 1.dp,
                            color = if (selectedColor) Color(0xFF1B4DD8) else Color(0x6656616F),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .combinedClickable(onClick = { onSelected(option.value) }),
                )
            }
        }
    }
}

@Composable
private fun FolderSpeakToggle(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF2F6FA))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Add folder to sentence", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (enabled) "Tapping this folder speaks it and adds it before opening." else "Tapping this folder only opens it.",
                fontSize = 12.sp,
                color = Color(0xFF56616F),
            )
        }
        OutlinedButton(onClick = onToggle) {
            Text(if (enabled) "on" else "off")
        }
    }
}

@Composable
private fun EditButtonDialog(item: VocabButton, onDismiss: () -> Unit, onSave: (VocabButton) -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var label by remember(item.id) { mutableStateOf(item.label) }
    var speech by remember(item.id) { mutableStateOf(item.speech) }
    var icon by remember(item.id) { mutableStateOf(item.icon) }
    var imagePath by remember(item.id) { mutableStateOf(item.imagePath) }
    var folderPath by remember(item.id) { mutableStateOf(item.boardId.orEmpty()) }
    var buttonColor by remember(item.id) { mutableStateOf(item.color) }
    var addToSentence by remember(item.id) { mutableStateOf(item.addToSentence) }
    var grammarRole by remember(item.id) { mutableStateOf(item.grammarRole) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imagePath = Store.copyImageToPrivateStorage(context, uri)
    }

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
                GrammarRoleDropdown(selected = grammarRole, onSelected = { grammarRole = it })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(icon, { icon = it.take(4) }, label = { Text("Icon text") })
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ButtonVisual(icon.ifBlank { "□" }, imagePath, Modifier.size(58.dp), 28)
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("choose image") }
                    if (imagePath != null) {
                        OutlinedButton(onClick = { imagePath = null }) { Text("remove") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(folderPath, { folderPath = it.lowercase(Locale.ROOT).filter { char -> char.isLetterOrDigit() || char == '_' }.take(20) }, label = { Text("Folder path, blank for normal word") })
                Text("Examples: want, need, food, drink, toilet, people, places, play, feel, body, things", fontSize = 12.sp, color = Color(0xFF56616F), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                ButtonColorPicker(selected = buttonColor, onSelected = { buttonColor = it })
                if (folderPath.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    FolderSpeakToggle(
                        enabled = addToSentence,
                        onToggle = { addToSentence = !addToSentence },
                    )
                }
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
                                color = buttonColor,
                                boardId = normalizedPath,
                                isCategory = normalizedPath != null,
                                imagePath = imagePath,
                                addToSentence = if (normalizedPath != null) addToSentence else true,
                                grammarRole = grammarRole,
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
