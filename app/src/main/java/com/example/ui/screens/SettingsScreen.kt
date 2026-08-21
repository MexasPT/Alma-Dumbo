package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.smtp.SmtpConfig
import com.example.smtp.SmtpResult
import com.example.smtp.SmtpSecurityType
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
import com.example.ui.viewmodel.SmtpOperationState
import com.example.ui.viewmodel.TranscriberViewModel

@Composable
fun SettingsScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf("Geral & IA", "SMTP", "Info & Sobre")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)
        ) {
            Text(
                text = "PAINEL DE CONTROLO",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontSize = 10.sp
                ),
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Definições do Sistema",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // Tabs Header
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SophisticatedSurface,
            contentColor = LavenderPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = LavenderPrimary,
                    height = 3.dp
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SophisticatedOutline)
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) LavenderPrimary else TextSecondary
                        )
                    },
                    icon = {
                        val icon = when (index) {
                            0 -> Icons.Default.Tune
                            1 -> Icons.Default.Email
                            else -> Icons.Default.Info
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) LavenderPrimary else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("settings_tab_$index")
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> GeneralSettingsTab(viewModel = viewModel)
            1 -> SmtpSettingsTab(viewModel = viewModel)
            2 -> InfoSettingsTab()
        }
    }
}

@Composable
private fun GeneralSettingsTab(viewModel: TranscriberViewModel) {
    val context = LocalContext.current
    val customApiKey by viewModel.customApiKey.collectAsState()
    var inputKey by remember(customApiKey) { mutableStateOf(customApiKey) }
    var autoTranslatePt by remember { mutableStateOf(true) }
    var highQualityRecord by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voice Selection (TTS) Card
        val ttsManager = viewModel.ttsManager
        val currentProfile by ttsManager.currentProfile.collectAsState()
        val isTtsSpeaking by ttsManager.isSpeaking.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, GlowLavender.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(LavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = LavenderOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Voz de Reprodução (TTS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Configuração da voz utilizada em Diálogo e Leituras",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Voices list with Gender sections
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Feminine Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VOZES FEMININAS (4)",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 9.5.sp),
                            color = GlowLavender,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toque em 'Demo' p/ ouvir",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextTertiary
                        )
                    }

                    com.example.audio.VoicePresets.FEMININE_VOICES.forEach { profile ->
                        val isSelected = currentProfile.id == profile.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ttsManager.setVoiceProfile(profile) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) LavenderContainer.copy(alpha = 0.5f) else SophisticatedSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) LavenderPrimary else SophisticatedOutline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) LavenderPrimary else Color.Transparent)
                                            .border(1.5.dp, if (isSelected) LavenderPrimary else TextSecondary, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                        Text(
                                            text = profile.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                    }
                                }

                                // Demo Button
                                OutlinedButton(
                                    onClick = { ttsManager.playDemo(profile) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir Demo",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Demo",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = LavenderPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Masculine Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VOZES MASCULINAS (4)",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontSize = 9.5.sp),
                            color = AmberGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tom grave & profundo",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TextTertiary
                        )
                    }

                    com.example.audio.VoicePresets.MASCULINE_VOICES.forEach { profile ->
                        val isSelected = currentProfile.id == profile.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ttsManager.setVoiceProfile(profile) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) LavenderContainer.copy(alpha = 0.5f) else SophisticatedSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) LavenderPrimary else SophisticatedOutline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) LavenderPrimary else Color.Transparent)
                                            .border(1.5.dp, if (isSelected) LavenderPrimary else TextSecondary, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                        Text(
                                            text = profile.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                    }
                                }

                                // Demo Button
                                OutlinedButton(
                                    onClick = { ttsManager.playDemo(profile) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir Demo",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Demo",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = LavenderPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gemini API Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(LavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = LavenderOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Chave Gemini API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Para transcrição e deteção multimodal",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("Insira a chave da API Gemini (opcional)...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline,
                        focusedContainerColor = SophisticatedSurfaceVariant,
                        unfocusedContainerColor = SophisticatedSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.setCustomApiKey(inputKey)
                        Toast.makeText(context, "Chave API atualizada!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderPrimary,
                        contentColor = DeepPurpleOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_api_key_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = DeepPurpleOnPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Chave", fontWeight = FontWeight.Bold, color = DeepPurpleOnPrimary)
                }
            }
        }

        // Preferences Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Preferências de Transcrição",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tradução Automática para Português",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Gera sempre a tradução em PT das falas detetadas noutras línguas",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = autoTranslatePt,
                        onCheckedChange = { autoTranslatePt = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LavenderPrimary,
                            checkedTrackColor = LavenderContainer,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SophisticatedSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gravação em Alta Fidelidade (AAC 44.1kHz)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Melhora a precisão na deteção de dialetos e sotaques subtis",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = highQualityRecord,
                        onCheckedChange = { highQualityRecord = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LavenderPrimary,
                            checkedTrackColor = LavenderContainer,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SophisticatedSurfaceVariant
                        )
                    )
                }
            }
        }

        // Storage & Catalog summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Armazenamento Local & Idiomas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Todos os ficheiros de áudio e transcrições ficam guardados no armazenamento local e base de dados SQLite/Room da aplicação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Suporte nativo completo a Português, Castelhano/Espanhol, Francês, Inglês, Italiano, Alemão, Holandês, Croata, Albanês, Dinamarquês, Finlandês, Tamazight (Berbere), Paquistanês, Indiano, Bengali, Árabe, Ucraniano, Moldavo, Romeno, Kimbundu, Coreano, Japonês, Tailandês, Vietnamita, Chinês, Russo e mais.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SmtpSettingsTab(viewModel: TranscriberViewModel) {
    val context = LocalContext.current
    val config by viewModel.smtpConfig.collectAsState()
    val smtpState by viewModel.smtpState.collectAsState()

    var senderName by remember(config.senderName) { mutableStateOf(config.senderName) }
    var senderEmail by remember(config.senderEmail) { mutableStateOf(config.senderEmail) }
    var host by remember(config.host) { mutableStateOf(config.host) }
    var portStr by remember(config.port) { mutableStateOf(config.port.toString()) }
    var securityType by remember(config.securityType) { mutableStateOf(config.securityType) }
    var requireAuth by remember(config.requireAuth) { mutableStateOf(config.requireAuth) }
    var username by remember(config.username) { mutableStateOf(config.username) }
    var password by remember(config.password) { mutableStateOf(config.password) }
    var defaultRecipient by remember(config.defaultRecipient) { mutableStateOf(config.defaultRecipient) }
    var showPassword by remember { mutableStateOf(false) }

    var testEmailInput by remember { mutableStateOf(defaultRecipient.ifBlank { senderEmail }) }
    val isSending = smtpState is SmtpOperationState.Sending

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SMTP Server Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(LavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = LavenderOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Servidor SMTP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Envio direto de transcrições e áudios",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nome do Remetente
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Nome do Remetente") },
                    placeholder = { Text("ex: Alma Dumbo / Empresa") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smtp_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email do Remetente
                OutlinedTextField(
                    value = senderEmail,
                    onValueChange = { senderEmail = it },
                    label = { Text("Email do Remetente") },
                    placeholder = { Text("ex: notificacoes@iterp.pt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smtp_email_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Host do Servidor
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host do Servidor SMTP") },
                    placeholder = { Text("ex: mail.iterp.pt ou smtp.gmail.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smtp_host_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Porta e Segurança
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Porta") },
                        placeholder = { Text("465") },
                        modifier = Modifier
                            .weight(0.4f)
                            .testTag("smtp_port_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = SophisticatedOutline
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Column(modifier = Modifier.weight(0.6f)) {
                        Text(
                            text = "Segurança / Cifragem",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SmtpSecurityType.values().forEach { sec ->
                                val isSelected = securityType == sec
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        securityType = sec
                                        portStr = sec.defaultPort.toString()
                                    },
                                    label = {
                                        Text(
                                            text = sec.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LavenderPrimary,
                                        selectedLabelColor = DeepPurpleOnPrimary,
                                        containerColor = SophisticatedSurfaceVariant,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Autenticação Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Requer Autenticação (Username & Password)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ativar para servidores que exigem login AUTH",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = requireAuth,
                        onCheckedChange = { requireAuth = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LavenderPrimary,
                            checkedTrackColor = LavenderContainer
                        )
                    )
                }

                if (requireAuth) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Utilizador / Username") },
                        placeholder = { Text("ex: utilizador@dominio.pt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smtp_user_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = SophisticatedOutline
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Palavra-passe / Password") },
                        placeholder = { Text("Palavra-passe de aplicação...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smtp_pass_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = SophisticatedOutline
                        ),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Mostrar Password",
                                    tint = TextSecondary
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Destinatário Predefinido
                OutlinedTextField(
                    value = defaultRecipient,
                    onValueChange = { defaultRecipient = it },
                    label = { Text("Destinatário Predefinido") },
                    placeholder = { Text("ex: arquivo@iterp.pt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smtp_default_recipient_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Guardar Definições SMTP
                Button(
                    onClick = {
                        val parsedPort = portStr.toIntOrNull() ?: securityType.defaultPort
                        val updated = SmtpConfig(
                            senderName = senderName,
                            senderEmail = senderEmail,
                            host = host,
                            port = parsedPort,
                            securityType = securityType,
                            requireAuth = requireAuth,
                            username = username,
                            password = password,
                            defaultRecipient = defaultRecipient
                        )
                        viewModel.updateSmtpConfig(updated)
                        Toast.makeText(context, "Configurações SMTP guardadas com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderPrimary,
                        contentColor = DeepPurpleOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_smtp_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = DeepPurpleOnPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Definições SMTP", fontWeight = FontWeight.Bold, color = DeepPurpleOnPrimary)
                }
            }
        }

        // Test Connection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Testar Conexão SMTP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Envia uma mensagem de teste para verificar a conectividade do servidor e a autenticação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = testEmailInput,
                    onValueChange = { testEmailInput = it },
                    label = { Text("Email para Receber Teste") },
                    placeholder = { Text("email.destino@empresa.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smtp_test_email_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val parsedPort = portStr.toIntOrNull() ?: securityType.defaultPort
                        val tempConfig = SmtpConfig(
                            senderName = senderName,
                            senderEmail = senderEmail,
                            host = host,
                            port = parsedPort,
                            securityType = securityType,
                            requireAuth = requireAuth,
                            username = username,
                            password = password,
                            defaultRecipient = defaultRecipient
                        )
                        viewModel.updateSmtpConfig(tempConfig)

                        viewModel.testSmtpConnection(testEmailInput.ifBlank { defaultRecipient.ifBlank { senderEmail } }) { res ->
                            when (res) {
                                is SmtpResult.Success -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                is SmtpResult.Failure -> Toast.makeText(context, "Falha: ${res.errorMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSending && host.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LavenderPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_smtp_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = LavenderPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("A testar servidor...", color = TextPrimary)
                    } else {
                        Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = null, tint = LavenderPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testar Conexão e Enviar Email", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Status message
                when (val state = smtpState) {
                    is SmtpOperationState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                Text(state.message, style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                            }
                        }
                    }
                    is SmtpOperationState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ListeningCoral.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ListeningCoral.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = ListeningCoral, modifier = Modifier.size(18.dp))
                                Text(state.errorMessage, style = MaterialTheme.typography.bodySmall, color = ListeningCoral)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun InfoSettingsTab() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Brand Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("info_hero_card"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LavenderContainer.copy(alpha = 0.45f),
                                SophisticatedSurface
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LavenderPrimary)
                                .border(1.5.dp, GlowLavender, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Logo Alma Dumbo",
                                tint = DeepPurpleOnPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Alma Dumbo",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    color = AmberGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.8.dp, AmberGold.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "PRO AI",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = AmberGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Inteligência Multimodal de Reconhecimento de Voz & Idiomas",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Desenvolvido para transcrição fonética avançada, identificação instantânea de dezenas de dialetos, modo Live em tempo real e envio por SMTP com mapa e áudio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Commercial Description Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("info_description_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(LavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = LavenderOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Descrição Comercial",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "O Alma Dumbo é uma plataforma corporativa e pessoal de inteligência acústica, transcrição contínua e análise multimodal de alta fidelidade.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Projetado para eliminar barreiras de comunicação em ambientes multiculturais, o Alma Dumbo processa gravações e fluxos de fala em direto com modelos de IA de última geração para detetar com precisão cirúrgica a língua e o dialeto falados — abrangendo desde os principais idiomas globais (Português, Castelhano, Francês, Inglês, Italiano, Alemão, Holandês, Croata, Albanês, Dinamarquês, Finlandês) até línguas regionais e tradicionais como o Tamazight (Berbere), o Kimbundu de Angola, o Bengali, o Urdu e línguas asiáticas.\n\nOferece transcrição verbatim na escrita nativa de cada idioma, tradução automática instantânea, resumos executivos, captura de geolocalização com mapa e exportação direta via SMTP a partir da aplicação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        // Specifications & Authorship Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("info_specs_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Especificações & Autoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Versão
                InfoDetailRow(
                    icon = Icons.Default.Layers,
                    label = "Versão da Aplicação",
                    value = "v1.0.0 (Build ${BuildConfig.VERSION_CODE})"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Versões Android Compatíveis
                InfoDetailRow(
                    icon = Icons.Default.Android,
                    label = "Versões Android Compatíveis",
                    value = "Android 8.0 Oreo (API 26) até Android 15 / 16 (API 36)"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Desenvolvido por
                InfoDetailRow(
                    icon = Icons.Default.Person,
                    label = "Desenvolvido por",
                    value = "Rodolfo Valentim by ITerp",
                    isHighlight = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Entidade e Marca (Mandatory Branding)
                InfoDetailRow(
                    icon = Icons.Default.CorporateFare,
                    label = "Entidade & Marca",
                    value = "ITerp - Tecnologias de Informação Lda\nAka Fábrica e Software",
                    isHighlight = true
                )
            }
        }

        // Tech specs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("info_tech_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(LavenderContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = LavenderOnContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Arquitetura e Protocolos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TechItemBullet("Motor de IA", "Google Gemini 2.5 Flash Multimodal Audio Understanding")
                TechItemBullet("Live Streaming", "Android SpeechRecognizer com dictation mode contínuo")
                TechItemBullet("Cliente SMTP", "RFC 5321/2045 com SSL (465) e TLS STARTTLS (587)")
                TechItemBullet("Geolocalização", "Android Geocoder com mapeamento de coordenadas GPS e OpenStreetMap")
                TechItemBullet("Base de Dados Local", "SQLite Room Database & Armazenamento Seguro")

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val infoText = """
                            Alma Dumbo
                            Entidade & Marca: ITerp - Tecnologias de Informação Lda | Aka Fábrica e Software
                            Desenvolvido por: Rodolfo Valentim by ITerp
                            Versão: v1.0.0 (Build ${BuildConfig.VERSION_CODE})
                            Compatibilidade: Android 8.0 (API 26) - Android 15/16 (API 36)
                            Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})
                        """.trimIndent()
                        clipboard.setPrimaryClip(ClipData.newPlainText("Info Alma Dumbo", infoText))
                        Toast.makeText(context, "Informações copiadas para a área de transferência!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SophisticatedOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = LavenderPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Diagnóstico do Sistema", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Copyright Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alma Dumbo • Rodolfo Valentim by ITerp",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "ITerp - Tecnologias de Informação Lda • Aka Fábrica e Software",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun InfoDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlight) LavenderContainer.copy(alpha = 0.35f) else SophisticatedSurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isHighlight) LavenderPrimary.copy(alpha = 0.5f) else SophisticatedOutline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isHighlight) LavenderPrimary else SophisticatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) DeepPurpleOnPrimary else LavenderPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isHighlight) LavenderPrimary else TextPrimary,
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TechItemBullet(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = LavenderPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = LavenderPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
