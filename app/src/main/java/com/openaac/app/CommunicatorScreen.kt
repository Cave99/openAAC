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
fun CommunicatorScreen(
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

    LaunchedEffect(currentProfile.id) {
        boardsById = Store.boards(context)
        draftBoards = boardsById
        sentence.clear()
        boardStack.clear()
        reorganizing = false
        showAddButton = false
        editingButtonIndex = null
        pendingDeleteIndex = null
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
            sentence.add(SentenceToken(button.label, button.speech, button.icon, button.imagePath))
            speakWord(button.speech)
            Store.trackWord(context, button.label)
            Store.trackTransition(context, previous, button.label)
            recommendationRefresh++
        }
        button.boardId?.let { boardStack.add(it) }
    }

    fun updateDraftBoard(boardId: String, nextButtons: List<VocabButton>) {
        draftBoards = draftBoards + (boardId to nextButtons.take(BoardEditor.MAX_BOARD_BUTTONS))
    }

    fun moveDraftButton(fromIndex: Int, toIndex: Int, action: DropAction) {
        draftBoards = BoardEditor.moveButton(
            boards = draftBoards,
            boardId = currentBoardId,
            fromIndex = fromIndex,
            toIndex = toIndex,
            action = action,
        )
    }

    fun addDraftButton(button: VocabButton) {
        draftBoards = BoardEditor.addButton(draftBoards, currentBoardId, button)
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
                        val spoken = sentence.joinToString(" ") { it.speech }
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
