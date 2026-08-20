package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
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

@Composable
fun InfoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Top Header
        Column {
            Text(
                text = "INFORMAÇÕES INSTITUCIONAIS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontSize = 10.sp
                ),
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sobre o Alma Dumbo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                        text = "Desenvolvido para transcrição fonética avançada, identificação instantânea de dezenas de dialetos e tradução em tempo real.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    text = "O Alma Dumbo é uma plataforma corporativa e pessoal de inteligência acústica e transcrição multimodal de alta fidelidade.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Projetado para eliminar barreiras de comunicação em ambientes multiculturais, o Alma Dumbo processa gravações de áudio com modelos de IA de última geração para detectar com precisão cirúrgica a língua e o dialeto falados — abrangendo desde os principais idiomas globais (Português, Castelhano, Francês, Inglês, Italiano, Alemão, Holandês, Croata, Albanês, Dinamarquês, Finlandês) até línguas regionais e tradicionais como o Tamazight (Berbere), o Kimbundu de Angola, o Bengali, o Urdu e línguas asiáticas.\n\nOferece transcrição verbatim na escrita nativa de cada idioma, tradução automática instantânea, resumos executivos e gestão de biblioteca 100% offline com persistência local de áudio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Specs & Metadata Card
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

                // Organização / Empresa
                InfoDetailRow(
                    icon = Icons.Default.CorporateFare,
                    label = "Entidade & Marca",
                    value = "ITerp Solutions & Innovation Lab"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technology Architecture Card
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
                        text = "Arquitetura e Tecnologias",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TechItemBullet("Motor de IA", "Google Gemini 2.5 Flash Multimodal Audio Understanding")
                TechItemBullet("Interface", "Android Jetpack Compose & Material Design 3")
                TechItemBullet("Gravação de Áudio", "Android MediaRecorder AAC / 44.1 kHz Estéreo")
                TechItemBullet("Base de Dados Local", "SQLite Room Database & Armazenamento Seguro")
                TechItemBullet("Privacidade", "Áudio encriptado localmente no dispositivo do utilizador")

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val infoText = """
                            Alma Dumbo
                            Versão: v1.0.0 (Build ${BuildConfig.VERSION_CODE})
                            Compatibilidade: Android 8.0 (API 26) - Android 15/16 (API 36)
                            Desenvolvido por: Rodolfo Valentim by ITerp
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

        Spacer(modifier = Modifier.height(24.dp))

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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Todos os direitos reservados © ${java.time.Year.now().value}",
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
