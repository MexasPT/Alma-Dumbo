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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.DialogueListeningStatus
import com.example.audio.DialogueManager
import com.example.audio.DialogueMessage
import com.example.audio.DialogueSpeaker
import com.example.audio.LanguageAutoDetector
import com.example.audio.LanguageMeta
import com.example.audio.SupportedLanguages
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlowLavender
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DialogueScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogueManager = viewModel.dialogueManager

    val messages by dialogueManager.messages.collectAsState()
    val status by dialogueManager.status.collectAsState()
    val currentPartnerLang by dialogueManager.currentPartnerLang.collectAsState()
    val isAutoSpeakEnabled by dialogueManager.isAutoSpeakEnabled.collectAsState()
    val liveSpokenText by dialogueManager.liveSpokenText.collectAsState()
    val detectedSpeaker by dialogueManager.detectedSpeaker.collectAsState()

    var manualInputText by remember { mutableStateOf("") }
    var isPartnerSelectorExpanded by remember { mutableStateOf(false) }

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
            dialogueManager.startListening()
        } else {
            Toast.makeText(context, "Permissão de microfone necessária para o modo Diálogo.", Toast.LENGTH_LONG).show()
        }
    }

    val isListening = status is DialogueListeningStatus.Listening ||
            status is DialogueListeningStatus.Translating ||
            status is DialogueListeningStatus.Speaking

    // Pulse animation for active listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

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
                    text = "CONVERSAÇÃO EM TEMPO REAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontSize = 9.5.sp
                    ),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Modo Diálogo Dumbo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto-speak audio toggle
                IconButton(
                    onClick = {
                        dialogueManager.toggleAutoSpeak()
                        val msg = if (!isAutoSpeakEnabled) "Áudio de tradução ativado" else "Áudio de tradução desativado"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isAutoSpeakEnabled) LavenderPrimary.copy(alpha = 0.2f) else SophisticatedSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isAutoSpeakEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Voz automática",
                        tint = if (isAutoSpeakEnabled) GlowLavender else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear history button
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = { dialogueManager.clearHistory() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar conversa",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Partner Language & Auto-detection info card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isPartnerSelectorExpanded = !isPartnerSelectorExpanded },
            shape = RoundedCornerShape(16.dp),
            color = SophisticatedSurface,
            border = BorderStroke(1.dp, if (isListening) GlowLavender.copy(alpha = 0.6f) else SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🇵🇹", fontSize = 22.sp)
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = GlowLavender,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = currentPartnerLang.flag, fontSize = 22.sp)

                        Column {
                            Text(
                                text = "DETEÇÃO AUTOMÁTICA ATIVA",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Português ⟷ ${currentPartnerLang.namePt}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LavenderPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, LavenderPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isPartnerSelectorExpanded) "Fechar" else "Alterar",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Expandable quick language picker
                AnimatedVisibility(visible = isPartnerSelectorExpanded) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Selecione o idioma do seu interlocutor ou deixe em automático:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SupportedLanguages.ALL) { lang ->
                                val isSelected = lang.code == currentPartnerLang.code
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) LavenderPrimary else SophisticatedSurfaceVariant,
                                    modifier = Modifier.clickable {
                                        dialogueManager.setPartnerLanguage(lang)
                                        isPartnerSelectorExpanded = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = lang.flag, fontSize = 12.sp)
                                        Text(
                                            text = lang.namePt,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = if (isSelected) DeepPurpleOnPrimary else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live status indicator pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (status) {
                is DialogueListeningStatus.Listening -> ListeningCoral
                is DialogueListeningStatus.Translating -> AmberGold
                is DialogueListeningStatus.Speaking -> EmeraldSuccess
                else -> TextSecondary
            }

            val statusText = when (status) {
                is DialogueListeningStatus.Listening -> "A OUVIR CONVERSA EM DIRETO..."
                is DialogueListeningStatus.Translating -> "A TRADUZIR COM IA..."
                is DialogueListeningStatus.Speaking -> "A FALAR TRADUÇÃO EM ÁUDIO..."
                else -> "MICROFONE EM PAUSA"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.15f),
                border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        ),
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live spoken interim bubble
        if (liveSpokenText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LavenderContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, GlowLavender.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = GlowLavender,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = liveSpokenText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages Conversation List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("dialogue_conversation_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(LavenderPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = LavenderPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Comece a falar livremente!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Quando falar em Português, o Olho do Dumbo traduz e fala na língua do convidado.\nQuando o convidado falar na sua língua, é traduzido e falado em Português.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        DialogueMessageBubble(
                            message = msg,
                            onPlayAudio = { dialogueManager.replayMessageAudio(msg) },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Diálogo Dumbo", "${msg.originalText} -> ${msg.translatedText}"))
                                Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Voice Control Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Manual text input
            OutlinedTextField(
                value = manualInputText,
                onValueChange = { manualInputText = it },
                placeholder = {
                    Text("Escreva uma frase...", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LavenderPrimary,
                    unfocusedBorderColor = SophisticatedOutline,
                    focusedContainerColor = SophisticatedSurface,
                    unfocusedContainerColor = SophisticatedSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                trailingIcon = {
                    if (manualInputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = manualInputText.trim()
                                manualInputText = ""
                                if (text.isNotBlank()) {
                                    scope.launch {
                                        dialogueManager.setApiKeyOverride(viewModel.customApiKey.value.ifBlank { null })
                                        // Process manually typed text
                                        val isPt = text.any { it in "áàãâéêíóôõúçÁÀÃÂÉÊÍÓÔÕÚÇ" } || LanguageAutoDetector.detect(text).languageCode.startsWith("pt")
                                        val targetLang = if (isPt) currentPartnerLang.code else "pt"
                                        val sourceLang = if (isPt) "pt" else currentPartnerLang.code

                                        val res = viewModel.speechService.translateText(
                                            text = text,
                                            sourceLang = sourceLang,
                                            targetLang = targetLang,
                                            apiKeyOverride = viewModel.customApiKey.value.ifBlank { null }
                                        )
                                        val translated = res.getOrDefault(text)

                                        val newMsg = DialogueMessage(
                                            speaker = if (isPt) DialogueSpeaker.ME else DialogueSpeaker.PARTNER,
                                            originalText = text,
                                            originalLangCode = sourceLang,
                                            originalLangName = if (isPt) "Português" else currentPartnerLang.namePt,
                                            originalFlag = if (isPt) "🇵🇹" else currentPartnerLang.flag,
                                            translatedText = translated,
                                            targetLangCode = targetLang,
                                            targetLangName = if (isPt) currentPartnerLang.namePt else "Português",
                                            targetFlag = if (isPt) currentPartnerLang.flag else "🇵🇹"
                                        )
                                        viewModel.dialogueManager.messages.let {
                                            // Append and speak
                                            dialogueManager.replayMessageAudio(newMsg)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = LavenderPrimary
                            )
                        }
                    }
                }
            )

            // Main Mic Button
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(ListeningCoral.copy(alpha = 0.35f))
                    )
                }

                Button(
                    onClick = {
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isListening) {
                                dialogueManager.stopListening()
                            } else {
                                dialogueManager.setApiKeyOverride(viewModel.customApiKey.value.ifBlank { null })
                                dialogueManager.startListening()
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) ListeningCoral else LavenderPrimary
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("dialogue_mic_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Parar" else "Ouvir",
                        tint = DeepPurpleOnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueMessageBubble(
    message: DialogueMessage,
    onPlayAudio: () -> Unit,
    onCopy: () -> Unit
) {
    val isMe = message.speaker == DialogueSpeaker.ME
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Speaker Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${message.originalFlag} ${if (isMe) "Eu (Português)" else "Convidado (${message.originalLangName})"}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = if (isMe) GlowLavender else AmberGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• $timeStr",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = TextTertiary
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) LavenderContainer.copy(alpha = 0.45f) else SophisticatedSurfaceVariant,
            border = BorderStroke(
                1.dp,
                if (isMe) GlowLavender.copy(alpha = 0.5f) else SophisticatedOutline.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Original spoken text
                Text(
                    text = message.originalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Translation Block
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMe) SophisticatedBackground.copy(alpha = 0.7f) else LavenderContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, GlowLavender.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = message.targetFlag, fontSize = 12.sp)
                                Text(
                                    text = "TRADUÇÃO (${message.targetLangName.uppercase()}):",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = if (isMe) AmberGold else GlowLavender,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = message.translatedText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            // TTS Playback
                            IconButton(
                                onClick = onPlayAudio,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Ouvir áudio",
                                    tint = LavenderPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Copy
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
