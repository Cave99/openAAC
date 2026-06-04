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


object Defaults {
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
        color: Long = 0xFFFFFFFF,
    ) = VocabButton(
        id = label.lowercase(Locale.ROOT).replace(" ", "_"),
        label = label,
        speech = label,
        icon = icon,
        color = color,
        boardId = boardId,
        isCategory = boardId != null,
    )

    private fun category(label: String, icon: String = "□") =
        word(label, icon, label, colorForFolder(label))

    fun allBoards(): Map<String, List<VocabButton>> = boards + (HOME_BOARD to home)

    private fun colorForFolder(label: String): Long =
        ButtonPalette.colors.firstOrNull { it.label.equals(label, ignoreCase = true) }?.value
            ?: ButtonPalette.colors[(kotlin.math.abs(label.hashCode()) % ButtonPalette.colors.size)].value
}

object ButtonPalette {
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
