package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.LiveStatus
import com.example.audio.LiveTranscriptSegment
import com.example.audio.SupportedLanguages
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlowLavender
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.ListeningCoral
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.TranscriberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveManager = viewModel.liveSpeechManager
    val ttsManager = viewModel.ttsManager
    val status by liveManager.status.collectAsState()
    val partialText by liveManager.partialText.collectAsState()
    val fullTranscript by liveManager.fullTranscript.collectAsState()
    val segments by liveManager.segments.collectAsState()
    val rmsLevel by liveManager.rmsLevel.collectAsState()
    val activeLangCode by liveManager.activeLanguage.collectAsState()

    val detectedLanguageMeta by liveManager.detectedLanguageMeta.collectAsState()
    val detectionConfidence by liveManager.detectionConfidence.collectAsState()
    val ptTranslation by viewModel.livePortugueseTranslation.collectAsState()
    val isTranslating by viewModel.isLiveTranslating.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            liveManager.startLiveListening()
        } else {
            Toast.makeText(context, "Permissão de microfone necessária para escuta em direto.", Toast.LENGTH_LONG).show()
        }
    }

    val isListening = status is LiveStatus.Listening

    // Sorted in reverse chronological order so newest translations appear at the TOP
    val reversedSegments = remember(segments) { segments.reversed() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRANSCRIÇÃO EM DIRETO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.5.sp,
                        fontSize = 10.sp
                    ),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live Speech & Tradução",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Live status badge
            Surface(
                color = if (isListening) ListeningCoral.copy(alpha = 0.15f) else SophisticatedSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isListening) ListeningCoral.copy(alpha = 0.6f) else SophisticatedOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isListening) ListeningCoral else TextSecondary)
                    )
                    Text(
                        text = if (isListening) "EM DIRETO" else "PAUSA",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isListening) ListeningCoral else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Automatic Language Detection Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SophisticatedSurface,
            border = BorderStroke(
                1.dp,
                if (isListening) GlowLavender.copy(alpha = 0.7f) else SophisticatedOutline
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(LavenderContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = detectedLanguageMeta.flag,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LÍNGUA DETETADA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LavenderPrimary.copy(alpha = 0.18f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "AUTO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = LavenderPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "${detectedLanguageMeta.namePt} (${detectedLanguageMeta.code.uppercase()})",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Confidence badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldSuccess.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, EmeraldSuccess.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "${(detectionConfidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Two-Column Table Header Labels
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SophisticatedSurfaceVariant.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1 Title (Left)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🌐",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "O QUE É DETETADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.5.sp
                        ),
                        color = GlowLavender
                    )
                }

                // Vertical Divider Indicator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(SophisticatedOutline)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Column 2 Title (Right)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🇵🇹",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "TRADUZIDO P/ PORTUGUÊS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.5.sp
                        ),
                        color = AmberGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Live Two-Column Dialogue View (Newest on TOP)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("live_transcript_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, if (isListening) GlowLavender.copy(alpha = 0.5f) else SophisticatedOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Waveform Level Bar when microphone is active
                if (isListening) {
                    LiveAudioLevelBar(rmsLevel = rmsLevel)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Top Actions inside Card (Clean / Copy All)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mais recentes no topo ⬆️",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextTertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isTranslating) {
                            Text(
                                text = "A traduzir...",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = GlowLavender,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        // Copy all
                        IconButton(
                            onClick = {
                                val fullText = buildString {
                                    reversedSegments.forEach { seg ->
                                        appendLine("🌐 [${seg.detectedLang.uppercase()}]: ${seg.text}")
                                        appendLine("🇵🇹 [PT]: ${seg.translationPt.ifBlank { seg.text }}")
                                        appendLine()
                                    }
                                }
                                if (fullText.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Olho do Dumbo Live", fullText))
                                    Toast.makeText(context, "Conversa completa copiada!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Tudo",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Clear
                        IconButton(
                            onClick = {
                                liveManager.clearTranscript()
                                viewModel.translateLiveTranscript("")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val hasContent = reversedSegments.isNotEmpty() || partialText.isNotBlank()

                if (!hasContent) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SophisticatedSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isListening) "A escutar em direto... Fale agora." else "Toque em 'Iniciar Escuta' para transcrever e traduzir em direto.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "As novas frases faladas aparecerão instantaneamente no topo!",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowLavender,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. ACTIVE LIVE UTTERANCE ROW (Currently being spoken right now)
                        if (partialText.isNotBlank()) {
                            item(key = "active_speaking_row") {
                                ActiveSpeakingLiveRow(
                                    partialText = partialText,
                                    ptTranslation = ptTranslation,
                                    isTranslating = isTranslating,
                                    detectedLang = activeLangCode,
                                    flag = detectedLanguageMeta.flag
                                )
                            }
                        }

                        // 2. COMPLETED DIALOGUE SEGMENTS (Reverse Order - Newest First!)
                        items(
                            items = reversedSegments,
                            key = { it.id }
                        ) { segment ->
                            TwoColumnDialogueItem(
                                segment = segment,
                                onSpeak = { textToSpeak ->
                                    ttsManager.speak(textToSpeak, "pt-PT")
                                },
                                onCopy = { textToCopy ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Live Segment", textToCopy))
                                    Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Control Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Start / Pause Button
            Button(
                onClick = {
                    if (!hasAudioPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        if (isListening) {
                            liveManager.pauseLiveListening()
                        } else {
                            liveManager.startLiveListening(activeLangCode)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) ListeningCoral else LavenderPrimary,
                    contentColor = DeepPurpleOnPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("live_toggle_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = DeepPurpleOnPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isListening) "Pausar Escuta" else "Iniciar Escuta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DeepPurpleOnPrimary
                )
            }

            // Save to History Button
            Button(
                onClick = {
                    val content = if (reversedSegments.isNotEmpty()) {
                        reversedSegments.joinToString("\n") {
                            "[${it.detectedLang.uppercase()}]: ${it.text} -> [PT]: ${it.translationPt.ifBlank { it.text }}"
                        }
                    } else {
                        fullTranscript.ifBlank { partialText }
                    }

                    if (content.isNotBlank()) {
                        viewModel.saveLiveSession(content, activeLangCode)
                        Toast.makeText(context, "Sessão guardada no Histórico!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Nenhum diálogo para guardar.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = reversedSegments.isNotEmpty() || fullTranscript.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedSurfaceVariant,
                    contentColor = TextPrimary,
                    disabledContainerColor = SophisticatedSurfaceVariant.copy(alpha = 0.4f),
                    disabledContentColor = TextSecondary.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, SophisticatedOutline),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("live_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = if (reversedSegments.isNotEmpty() || fullTranscript.isNotBlank()) LavenderPrimary else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActiveSpeakingLiveRow(
    partialText: String,
    ptTranslation: String,
    isTranslating: Boolean,
    detectedLang: String,
    flag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GlowLavender.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, GlowLavender.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ListeningCoral)
                    )
                    Text(
                        text = "EM TEMPO REAL (A FALAR...)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = ListeningCoral
                    )
                }

                Text(
                    text = "$flag ${detectedLang.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Two-column active row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1: Detected partial text
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SophisticatedSurface.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = partialText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                }

                // Column 2: Portuguese translation
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(LavenderContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .border(1.dp, LavenderPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (ptTranslation.isNotBlank()) {
                        Text(
                            text = ptTranslation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = GlowLavender
                        )
                    } else {
                        Text(
                            text = if (isTranslating) "A traduzir..." else "A aguardar tradução...",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoColumnDialogueItem(
    segment: LiveTranscriptSegment,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    val timeFormatted = remember(segment.timestampMs) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(segment.timestampMs))
    }

    val meta = remember(segment.detectedLang) {
        SupportedLanguages.findByCode(segment.detectedLang) ?: SupportedLanguages.ALL.first()
    }

    val portugueseText = segment.translationPt.ifBlank { segment.text }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SophisticatedSurfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(0.8.dp, SophisticatedOutline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Timestamp and Lang meta header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = meta.flag, fontSize = 12.sp)
                    Text(
                        text = "${meta.namePt} (${meta.code.uppercase()})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Two Columns Side by Side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1 (Left): Detected Original Text
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SophisticatedSurface, RoundedCornerShape(10.dp))
                        .border(0.5.dp, SophisticatedOutline, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onCopy(segment.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Original",
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Column 2 (Right): Translated to Portuguese
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(LavenderContainer.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .border(1.dp, LavenderPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = portugueseText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play TTS in Portuguese
                        IconButton(
                            onClick = { onSpeak(portugueseText) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Ouvir em Português",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { onCopy(portugueseText) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Tradução",
                                tint = LavenderPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAudioLevelBar(rmsLevel: Float) {
    val barColor by animateColorAsState(
        targetValue = if (rmsLevel > 0.6f) ListeningCoral else GlowLavender,
        label = "level_color"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val totalBars = 28
        val spacing = 3.dp.toPx()
        val totalSpacing = spacing * (totalBars - 1)
        val barWidth = (size.width - totalSpacing) / totalBars

        for (i in 0 until totalBars) {
            val centerDist = kotlin.math.abs(i - totalBars / 2f) / (totalBars / 2f)
            val normalizedFactor = (1f - centerDist * 0.7f).coerceIn(0.2f, 1f)
            val barHeight = ((rmsLevel * normalizedFactor * size.height).coerceAtLeast(2.dp.toPx()))

            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2

            drawRoundRect(
                color = barColor.copy(alpha = (0.35f + rmsLevel * 0.65f).coerceIn(0.3f, 1f)),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}
