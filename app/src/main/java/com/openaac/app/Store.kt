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


object Store {
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
        val events = JSONArray(prefs(context).getString(profileKey(context, KEY_WORD_EVENTS), "[]") ?: "[]")
        val sentences = JSONArray(prefs(context).getString(profileKey(context, KEY_SENTENCES), "[]") ?: "[]")
        return UsageInsightsCalculator.summarize(
            usageCounts = counts.toIntMap(),
            wordEvents = List(events.length()) { index ->
                val item = events.getJSONObject(index)
                WordEvent(at = item.optLong("at", 0L), word = item.optString("word"))
            },
            spokenSentences = List(sentences.length()) { index ->
                sentences.getJSONObject(index).optString("spoken")
            },
            nowMs = now,
        )
    }

    fun recommendations(
        context: Context,
        lastWord: String?,
        currentBoard: String?,
        visibleButtons: List<VocabButton>,
        boards: Map<String, List<VocabButton>>,
    ): RecommendationResult {
        val counts = JSONObject(prefs(context).getString(profileKey(context, KEY_COUNTS), "{}") ?: "{}")
        val transitions = JSONObject(prefs(context).getString(profileKey(context, KEY_TRANSITIONS), "{}") ?: "{}")
        return RecommendationEngine.recommend(
            lastWord = lastWord,
            currentBoard = currentBoard,
            visibleButtons = visibleButtons,
            boards = boards,
            usageCounts = counts.toIntMap(),
            transitionCounts = transitions.toTransitionMap(),
        )
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
        return BoardEditor.applySharedVisuals(boards, source)
    }

    private fun transitionKey(previous: String, next: String) = "${previous.normalized()}>${next.normalized()}"

    private fun String.normalized() = lowercase(Locale.ROOT).trim()

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
    )

    private fun JSONObject.toIntMap(): Map<String, Int> =
        keys().asSequence().associateWith { key -> optInt(key) }

    private fun JSONObject.toTransitionMap(): Map<Pair<String, String>, Int> =
        keys().asSequence().mapNotNull { key ->
            val split = key.split(">")
            if (split.size == 2) (split[0] to split[1]) to optInt(key) else null
        }.toMap()
}
