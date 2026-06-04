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
fun ReorganizeBar(
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
fun SentenceBar(
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
fun SentenceChip(token: SentenceToken, selected: Boolean, onTap: () -> Unit) {
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
fun ButtonGrid(
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
fun ReorderableVocabTile(
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
fun VocabTile(
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
fun PinnedStrip(onTap: (VocabButton) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Defaults.pinned.forEach { button ->
            VocabTile(button, Modifier.weight(1f).fillMaxHeight(), onTap)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestionsPanel(result: RecommendationResult, onTap: (VocabButton) -> Unit) {
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
fun NavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(76.dp)) {
        Text(label, fontSize = 18.sp)
    }
}

@Composable
fun ActionButton(label: String, icon: String, color: Color, onClick: () -> Unit) {
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
fun ButtonVisual(icon: String, imagePath: String?, modifier: Modifier, iconFontSize: Int) {
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
fun BackspaceButton(onBackspace: () -> Unit) {
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

