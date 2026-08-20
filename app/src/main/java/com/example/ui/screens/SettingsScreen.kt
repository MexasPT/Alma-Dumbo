package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TranscriberViewModel

@Composable
fun SettingsScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customApiKey by viewModel.customApiKey.collectAsState()
    var inputKey by remember(customApiKey) { mutableStateOf(customApiKey) }
    var autoTranslatePt by remember { mutableStateOf(true) }
    var highQualityRecord by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column {
            Text(
                text = "CONFIGURAÇÃO DO SISTEMA",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontSize = 10.sp
                ),
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Definições e API",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // API Key Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline)
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
                            text = "Para transcrição e detecção multimodal",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Preferences Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Preferências de Transcrição",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auto translate switch
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

                // High quality audio
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

        Spacer(modifier = Modifier.height(16.dp))

        // About / Storage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedOutline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Armazenamento & Idiomas",
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
