package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TranscriptionEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.ListeningCoral
import com.example.ui.theme.ListeningCoralContainer
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.SmtpOperationState
import java.io.File

@Composable
fun SmtpSendDialog(
    record: TranscriptionEntity,
    defaultRecipient: String,
    isSmtpConfigured: Boolean,
    smtpState: SmtpOperationState,
    onDismiss: () -> Unit,
    onSend: (recipient: String, includeAudio: Boolean, includeLocation: Boolean) -> Unit
) {
    var recipientEmail by remember { mutableStateOf(defaultRecipient) }
    var includeAudio by remember { mutableStateOf(record.audioFilePath.isNotBlank()) }
    var includeLocation by remember { mutableStateOf(record.locationAddress != null || record.latitude != null) }

    val hasAudioFile = record.audioFilePath.isNotBlank() && File(record.audioFilePath).exists()
    val isSending = smtpState is SmtpOperationState.Sending

    AlertDialog(
        containerColor = SophisticatedSurface,
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = LavenderPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Enviar via Email (SMTP)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isSmtpConfigured) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ListeningCoralContainer.copy(alpha = 0.2f))
                            .border(1.dp, ListeningCoral.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ListeningCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Atenção: O servidor SMTP ainda não foi configurado no menu Definições. O envio pode falhar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ListeningCoral
                            )
                        }
                    }
                }

                Text(
                    text = "O email será enviado diretamente pelo servidor SMTP da aplicação com o relatório detalhado da gravação (${record.detectedLanguage} ${record.flagEmoji}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = { Text("Email Destinatário") },
                    placeholder = { Text("exemplo@dominio.com") },
                    singleLine = true,
                    enabled = !isSending,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smtp_recipient_input")
                )

                // Attachment Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SophisticatedSurfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, SophisticatedOutline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Audio Attachment Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasAudioFile && !isSending) {
                                includeAudio = !includeAudio
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeAudio && hasAudioFile,
                            onCheckedChange = { if (hasAudioFile) includeAudio = it },
                            enabled = hasAudioFile && !isSending,
                            colors = CheckboxDefaults.colors(checkedColor = LavenderPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (hasAudioFile) LavenderPrimary else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Anexar ficheiro de áudio (.m4a)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasAudioFile) TextPrimary else TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                            if (!hasAudioFile) {
                                Text(
                                    text = "Sem ficheiro local disponível",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = TextTertiary
                                )
                            }
                        }
                    }

                    // Location Checkbox
                    val hasLocation = record.locationAddress != null || record.latitude != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSending) {
                                includeLocation = !includeLocation
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeLocation,
                            onCheckedChange = { includeLocation = it },
                            enabled = !isSending,
                            colors = CheckboxDefaults.colors(checkedColor = LavenderPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (hasLocation) AmberGold else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Incluir Morada e Mapa GPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            if (record.locationAddress != null) {
                                Text(
                                    text = record.locationAddress,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Sending Progress
                if (isSending) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LavenderPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = (smtpState as SmtpOperationState.Sending).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = LavenderPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(recipientEmail.trim(), includeAudio && hasAudioFile, includeLocation)
                },
                enabled = !isSending && recipientEmail.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LavenderPrimary,
                    contentColor = DeepPurpleOnPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_send_smtp_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = DeepPurpleOnPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Enviar Agora",
                    fontWeight = FontWeight.Bold,
                    color = DeepPurpleOnPrimary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
