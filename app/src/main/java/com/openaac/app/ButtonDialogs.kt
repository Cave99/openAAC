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
fun AddButtonDialog(
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
fun ButtonColorPicker(selected: Long, onSelected: (Long) -> Unit) {
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
fun FolderSpeakToggle(enabled: Boolean, onToggle: () -> Unit) {
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
fun GrammarRoleDropdown(
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
    }
}

@Composable
fun EditButtonDialog(item: VocabButton, onDismiss: () -> Unit, onSave: (VocabButton) -> Unit) {
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
fun FullScreenOverlay(content: @Composable () -> Unit) {
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
