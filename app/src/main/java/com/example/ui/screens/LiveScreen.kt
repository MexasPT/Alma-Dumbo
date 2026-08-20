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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.LiveStatus
import com.example.audio.SupportedLanguages
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlowLavender
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.ListeningCoral
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.TranscriberViewModel

@Composable
fun LiveScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveManager = viewModel.liveSpeechManager
    val status by liveManager.status.collectAsState()
    val partialText by liveManager.partialText.collectAsState()
    val fullTranscript by liveManager.fullTranscript.collectAsState()
    val rmsLevel by liveManager.rmsLevel.collectAsState()
    val activeLangCode by liveManager.activeLanguage.collectAsState()

    val ptTranslation by viewModel.livePortugueseTranslation.collectAsState()
    val isTranslating by viewModel.isLiveTranslating.collectAsState()

    // Display mode: 0 -> Português, 1 -> Original, 2 -> Ambos (Bilingue)
    var displayMode by remember { mutableIntStateOf(0) }

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
            liveManager.startLiveListening(activeLangCode)
        } else {
            Toast.makeText(context, "Permissão de microfone necessária para escuta em direto.", Toast.LENGTH_LONG).show()
        }
    }

    val isListening = status is LiveStatus.Listening

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
            .padding(horizontal = 18.dp, vertical = 12.dp)
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
                    text = "Live Speech AI",
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

        Spacer(modifier = Modifier.height(12.dp))

        // Language Quick Switch Carousel
        Text(
            text = "IDIOMA FALADO (ENTRADA):",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 1.5.sp),
            color = TextTertiary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        val languages = listOf(
            "pt-PT" to "🇵🇹 Português (PT)",
            "pt-BR" to "🇧🇷 Português (BR)",
            "es-ES" to "🇪🇸 Castelhano / Espanhol",
            "fr-FR" to "🇫🇷 Francês",
            "en-US" to "🇬🇧 Inglês",
            "it-IT" to "🇮🇹 Italiano",
            "de-DE" to "🇩🇪 Alemão",
            "nl-NL" to "🇳🇱 Holandês",
            "hr-HR" to "🇭🇷 Croata",
            "sq-AL" to "🇦🇱 Albanês",
            "da-DK" to "🇩🇰 Dinamarquês",
            "fi-FI" to "🇫🇮 Finlandês",
            "ar-SA" to "🇸🇦 Árabe",
            "ur-PK" to "🇵🇰 Urdu",
            "hi-IN" to "🇮🇳 Hindi",
            "uk-UA" to "🇺🇦 Ucraniano",
            "ro-RO" to "🇷🇴 Romeno / Moldavo",
            "zh-CN" to "🇨🇳 Chinês",
            "ja-JP" to "🇯🇵 Japonês",
            "ko-KR" to "🇰🇷 Coreano"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { (code, label) ->
                val isSelected = activeLangCode == code
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) LavenderContainer else SophisticatedSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) LavenderPrimary else SophisticatedOutline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.clickable {
                        liveManager.setLanguage(code)
                    }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) LavenderOnContainer else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Display Mode Switcher (Português / Original / Ambos)
        Surface(
            color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to "🇵🇹 Em Português",
                    1 to "🌐 Original",
                    2 to "⚡ Bilingue"
                ).forEach { (mode, label) ->
                    val isModeSelected = displayMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { displayMode = mode },
                        color = if (isModeSelected) LavenderPrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isModeSelected) DeepPurpleOnPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Live Transcript Display Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("live_transcript_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, if (isListening) GlowLavender.copy(alpha = 0.6f) else SophisticatedOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header inside Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isListening) GlowLavender else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (displayMode) {
                                0 -> "Tradução em Português (PT)"
                                1 -> "Texto Falado Original"
                                else -> "Texto Original & Tradução Portuguesa"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTranslating) {
                            Text(
                                text = "A traduzir...",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = GlowLavender
                            )
                        }

                        IconButton(
                            onClick = {
                                val textToCopy = when (displayMode) {
                                    0 -> ptTranslation.ifBlank { fullTranscript.ifBlank { partialText } }
                                    1 -> fullTranscript.ifBlank { partialText }
                                    else -> "ORIGINAL:\n${fullTranscript.ifBlank { partialText }}\n\nTRADUÇÃO (PT):\n$ptTranslation"
                                }
                                if (textToCopy.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Alma Dumbo Live", textToCopy))
                                    Toast.makeText(context, "Texto copiado!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                liveManager.clearTranscript()
                                viewModel.translateLiveTranscript("")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Waveform level indicator when listening
                if (isListening) {
                    LiveAudioLevelBar(rmsLevel = rmsLevel)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Scrollable text area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(SophisticatedSurfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val hasAnyContent = fullTranscript.isNotBlank() || partialText.isNotBlank() || ptTranslation.isNotBlank()

                    if (!hasAnyContent) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isListening) "A escutar... Fale agora próximo ao microfone." else "Toque no botão 'Iniciar Escuta' para transcrever e traduzir em direto para Português.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Mode 0: Only Portuguese Translation
                            if (displayMode == 0) {
                                if (ptTranslation.isNotBlank()) {
                                    Text(
                                        text = ptTranslation,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 17.sp,
                                            lineHeight = 26.sp
                                        ),
                                        color = TextPrimary
                                    )
                                } else if (fullTranscript.isNotBlank() || partialText.isNotBlank()) {
                                    // Showing spoken text while translation arrives
                                    Text(
                                        text = fullTranscript.ifBlank { partialText },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 17.sp,
                                            lineHeight = 26.sp
                                        ),
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "A gerar tradução em Português...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlowLavender
                                    )
                                }
                            }

                            // Mode 1: Only Original Language
                            else if (displayMode == 1) {
                                if (fullTranscript.isNotBlank()) {
                                    Text(
                                        text = fullTranscript,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 17.sp,
                                            lineHeight = 26.sp
                                        ),
                                        color = TextPrimary
                                    )
                                }
                                if (partialText.isNotBlank()) {
                                    Text(
                                        text = partialText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 17.sp,
                                            lineHeight = 26.sp
                                        ),
                                        color = GlowLavender,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Mode 2: Bilingual (Both Original & Portuguese)
                            else {
                                // Original Box
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SophisticatedSurface, RoundedCornerShape(12.dp))
                                        .border(0.5.dp, SophisticatedOutline, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "🌐 IDIOMA ORIGINAL (${activeLangCode.uppercase()}):",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                                        color = TextTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullTranscript.ifBlank { partialText.ifBlank { "(A aguardar fala...)" } },
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = TextSecondary
                                    )
                                }

                                // Portuguese Translation Box
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LavenderContainer.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .border(1.dp, LavenderPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "🇵🇹 TRADUÇÃO EM PORTUGUÊS:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                                        color = LavenderPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ptTranslation.ifBlank { if (isTranslating) "A traduzir..." else fullTranscript.ifBlank { "(A aguardar tradução...)" } },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Control Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Start / Pause / Stop Button
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
                    .height(54.dp)
                    .testTag("live_toggle_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = DeepPurpleOnPrimary,
                    modifier = Modifier.size(24.dp)
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
                    val content = fullTranscript.ifBlank { partialText }
                    if (content.isNotBlank()) {
                        viewModel.saveLiveSession(content, activeLangCode)
                        Toast.makeText(context, "Sessão Live guardada com sucesso no Histórico!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Nenhum texto para guardar.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = fullTranscript.isNotBlank() || partialText.isNotBlank() || ptTranslation.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedSurfaceVariant,
                    contentColor = TextPrimary,
                    disabledContainerColor = SophisticatedSurfaceVariant.copy(alpha = 0.4f),
                    disabledContentColor = TextSecondary.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, SophisticatedOutline),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("live_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = if (fullTranscript.isNotBlank() || partialText.isNotBlank() || ptTranslation.isNotBlank()) LavenderPrimary else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar", fontWeight = FontWeight.SemiBold)
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
            .height(14.dp)
    ) {
        val totalBars = 32
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (totalBars - 1)
        val barWidth = (size.width - totalSpacing) / totalBars

        for (i in 0 until totalBars) {
            val centerDist = kotlin.math.abs(i - totalBars / 2f) / (totalBars / 2f)
            val normalizedFactor = (1f - centerDist * 0.7f).coerceIn(0.2f, 1f)
            val barHeight = ((rmsLevel * normalizedFactor * size.height).coerceAtLeast(3.dp.toPx()))

            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2

            drawRoundRect(
                color = barColor.copy(alpha = (0.35f + rmsLevel * 0.65f).coerceIn(0.3f, 1f)),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
