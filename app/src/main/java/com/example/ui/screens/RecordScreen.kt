package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SupportedLanguages
import com.example.data.db.TranscriptionEntity
import com.example.smtp.SmtpResult
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.LanguageBadge
import com.example.ui.components.LiveAudioWaveform
import com.example.ui.components.MicrophonePermissionBanner
import com.example.ui.components.MicrophonePermissionWrapper
import com.example.ui.components.RecordingPulseRing
import com.example.ui.components.ScriptTag
import com.example.ui.components.SmtpSendDialog
import com.example.ui.components.formatDuration
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.GlowLavender
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.ListeningCoral
import com.example.ui.theme.ListeningCoralContainer
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.RecordingUiState
import com.example.ui.viewmodel.TranscriberViewModel

@Composable
fun RecordScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsState()
    val isRecordingManagerActive by viewModel.recorderManager.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recorderManager.recordingDurationMs.collectAsState()
    val amplitudeHistory by viewModel.recorderManager.amplitudeHistory.collectAsState()
    val playbackInfo by viewModel.playbackState.collectAsState()
    val smtpConfig by viewModel.smtpConfig.collectAsState()
    val smtpState by viewModel.smtpState.collectAsState()

    var recordToSendSmtp by remember { mutableStateOf<TranscriptionEntity?>(null) }

    val isLiveRecording = recordingState is RecordingUiState.Recording

    // SMTP Dialog on Record Screen
    recordToSendSmtp?.let { record ->
        SmtpSendDialog(
            record = record,
            defaultRecipient = smtpConfig.defaultRecipient,
            isSmtpConfigured = smtpConfig.host.isNotBlank(),
            smtpState = smtpState,
            onDismiss = { recordToSendSmtp = null },
            onSend = { targetEmail, includeAudio, includeLocation ->
                viewModel.sendRecordViaSmtp(
                    record = record,
                    recipientEmail = targetEmail,
                    includeAudio = includeAudio,
                    includeLocation = includeLocation
                ) { result ->
                    when (result) {
                        is SmtpResult.Success -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            recordToSendSmtp = null
                        }
                        is SmtpResult.Failure -> {
                            Toast.makeText(context, "Erro: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    MicrophonePermissionWrapper(
        onPermissionGranted = {
            // Ready to record
        }
    ) { hasPermission, requestPermission ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SophisticatedBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sophisticated App Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ALMA DUMBO AI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.5.sp,
                            fontSize = 10.sp
                        ),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isLiveRecording) "Transcrição Ativa" else "Detetor de Línguas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Live status indicator badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SophisticatedSurface)
                        .border(1.dp, SophisticatedOutline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLiveRecording) ListeningCoral else LavenderPrimary)
                        )
                        Text(
                            text = if (isLiveRecording) "Ouvindo" else "25+ Línguas",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLiveRecording) ListeningCoral else TextTertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Interactive Transcription Surface
            if (!hasPermission) {
                MicrophonePermissionBanner(onRequestPermission = requestPermission)
            } else {
                when (val state = recordingState) {
                    is RecordingUiState.Idle -> {
                        IdleSophisticatedSurface(
                            onStartRecording = { viewModel.startRecording() }
                        )
                    }

                    is RecordingUiState.Recording -> {
                        ActiveRecordingSurface(
                            durationMs = recordingDurationMs,
                            amplitudeHistory = amplitudeHistory,
                            onStop = { viewModel.stopAndTranscribe() },
                            onCancel = { viewModel.cancelRecording() }
                        )
                    }

                    is RecordingUiState.Processing -> {
                        ProcessingAudioSurface(
                            message = state.message,
                            durationMs = state.durationMs
                        )
                    }

                    is RecordingUiState.Success -> {
                        ResultSuccessSurface(
                            record = state.record,
                            playbackInfo = playbackInfo,
                            onPlayPause = { viewModel.playAudio(state.record) },
                            onSeek = { viewModel.seekAudio(it) },
                            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(state.record) },
                            onSendSmtp = { recordToSendSmtp = state.record },
                            onRecordAnother = { viewModel.resetRecordingState() }
                        )
                    }

                    is RecordingUiState.Error -> {
                        ErrorSophisticatedSurface(
                            errorMessage = state.errorMessage,
                            canRetry = state.canRetryFile != null,
                            onRetry = {
                                state.canRetryFile?.let { viewModel.retryAnalysis(it) }
                            },
                            onDismiss = { viewModel.resetRecordingState() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recent / Supported Languages Horizontal Shortcuts Row
            SupportedLanguagesShortcutsRow()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IdleSophisticatedSurface(
    onStartRecording: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = GlowLavender),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SophisticatedOutline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top tag inside card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LavenderContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Auto-Detetor",
                        style = MaterialTheme.typography.labelSmall,
                        color = LavenderOnContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Elegant Serif Prompt
            Text(
                text = "O sistema está pronto para escutar...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                ),
                color = TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"Fale em qualquer idioma para transcrever e traduzir instantaneamente com inteligência artificial.\"",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Ambient Waveform Preview
            LiveAudioWaveform(
                amplitudes = emptyList(),
                isRecording = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                height = 54.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Large Record Button with glow
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(20.dp, RoundedCornerShape(28.dp), spotColor = LavenderPrimary)
                    .clip(RoundedCornerShape(28.dp))
                    .background(LavenderPrimary)
                    .clickable { onStartRecording() }
                    .testTag("start_recording_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Começar a Gravar",
                    tint = DeepPurpleOnPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Toque para Gravar Áudio",
                style = MaterialTheme.typography.labelMedium,
                color = LavenderPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActiveRecordingSurface(
    durationMs: Long,
    amplitudeHistory: List<Float>,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = ListeningCoral.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            ListeningCoral.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ListeningCoralContainer.copy(alpha = 0.35f))
                        .border(1.dp, ListeningCoral.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        text = "A CAPTAR SINAIS DE VOZ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ListeningCoral,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Serif dynamic transcription prompt
            Text(
                text = "O modelo de IA está a analisar os fonemas e dialetos em tempo real...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 17.sp,
                    lineHeight = 24.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Real-time Soundwave Visualizer
            LiveAudioWaveform(
                amplitudes = amplitudeHistory,
                isRecording = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                height = 70.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Stop / Complete Button
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                RecordingPulseRing(isRecording = true, size = 110.dp)

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = ListeningCoral)
                        .clip(RoundedCornerShape(26.dp))
                        .background(ListeningCoral)
                        .clickable { onStop() }
                        .testTag("stop_recording_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Parar e Transcrever",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Toque para finalizar e obter transcrição",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline),
                modifier = Modifier.testTag("cancel_recording_button")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancelar Gravação", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ProcessingAudioSurface(
    message: String,
    durationMs: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = GlowLavender),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SophisticatedOutline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                color = LavenderPrimary,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Identificando idioma falado e gerando transcrição fonética (${formatDuration(durationMs)})",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultSuccessSurface(
    record: TranscriptionEntity,
    playbackInfo: com.example.audio.PlaybackInfo,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onSendSmtp: () -> Unit,
    onRecordAnother: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = GlowLavender),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SophisticatedOutline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Header with language detection chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageBadge(
                    flagEmoji = record.flagEmoji,
                    languageName = record.detectedLanguage,
                    confidence = record.confidence
                )
            }

            if (record.languageScript.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ScriptTag(scriptName = "Escrita: ${record.languageScript}")
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Audio Player Component
            AudioPlayerCard(
                recordId = record.id,
                playbackInfo = playbackInfo,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
                onSpeedChange = onSpeedChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs for Original vs Translation vs Summary
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = LavenderPrimary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Original (${record.flagEmoji})",
                            color = if (selectedTab == 0) LavenderPrimary else TextSecondary,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                if (record.translationPt.isNotBlank()) {
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Português 🇵🇹",
                                color = if (selectedTab == 1) LavenderPrimary else TextSecondary,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
                if (record.summary.isNotBlank()) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Resumo",
                                color = if (selectedTab == 2) LavenderPrimary else TextSecondary,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content with Sophisticated Serif Typography
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.padding(18.dp)) {
                    when (selectedTab) {
                        0 -> {
                            Column {
                                Text(
                                    text = "\"${record.transcription}\"",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                        1 -> {
                            Column {
                                Text(
                                    text = "\"${record.translationPt}\"",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                        2 -> {
                            Column {
                                Text(
                                    text = record.summary,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp
                                    ),
                                    color = TextPrimary
                                )
                                if (record.keywords.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Tópicos: ${record.keywords}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LavenderPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Send via SMTP Primary Button
            Button(
                onClick = onSendSmtp,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LavenderPrimary,
                    contentColor = DeepPurpleOnPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("record_screen_send_smtp_button")
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Email,
                    contentDescription = null,
                    tint = DeepPurpleOnPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enviar Registo via Email (SMTP)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepPurpleOnPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Copy, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val textToCopy = when (selectedTab) {
                            1 -> record.translationPt
                            2 -> record.summary
                            else -> record.transcription
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("VozLíngua Transcrição", textToCopy)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Transcrição copiada!", Toast.LENGTH_SHORT).show()
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_transcription_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                                    [VozLíngua - Idioma: ${record.detectedLanguage} ${record.flagEmoji}]
                                    
                                    Transcrição Original:
                                    ${record.transcription}
                                    
                                    ${if (record.translationPt.isNotBlank()) "Tradução (PT):\n" + record.translationPt else ""}
                                """.trimIndent()
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partilhar Transcrição"))
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_transcription_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partilhar", color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onRecordAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_another_button"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.5f))
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = LavenderPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gravar Novo Áudio",
                    fontWeight = FontWeight.SemiBold,
                    color = LavenderPrimary
                )
            }
        }
    }
}

@Composable
private fun SupportedLanguagesShortcutsRow() {
    val languages = SupportedLanguages.ALL
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ATALHOS:",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontSize = 10.sp
            ),
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { lang ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SophisticatedSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        SophisticatedOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = lang.flag, fontSize = 14.sp)
                        Text(
                            text = lang.namePt.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorSophisticatedSurface(
    errorMessage: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = ListeningCoralContainer),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ListeningCoral.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ListeningCoral,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Erro no Reconhecimento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ListeningCoral
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canRetry) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary),
                        modifier = Modifier.testTag("retry_analysis_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = DeepPurpleOnPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tentar Novamente", color = DeepPurpleOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline),
                    modifier = Modifier.testTag("dismiss_error_button")
                ) {
                    Text("Fechar", color = TextSecondary)
                }
            }
        }
    }
}
