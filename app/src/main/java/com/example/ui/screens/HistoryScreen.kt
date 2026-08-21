package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachEmail
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SupportedLanguages
import com.example.data.db.TranscriptionEntity
import com.example.smtp.SmtpResult
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.LanguageBadge
import com.example.ui.components.SmtpSendDialog
import com.example.ui.components.formatDuration
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
import com.example.ui.viewmodel.SmtpOperationState
import com.example.ui.viewmodel.TranscriberViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLanguageFilter by viewModel.selectedLanguageFilter.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val playbackInfo by viewModel.playbackState.collectAsState()
    val smtpConfig by viewModel.smtpConfig.collectAsState()
    val smtpState by viewModel.smtpState.collectAsState()

    var recordToDelete by remember { mutableStateOf<TranscriptionEntity?>(null) }
    var recordToEdit by remember { mutableStateOf<TranscriptionEntity?>(null) }
    var recordToSendSmtp by remember { mutableStateOf<TranscriptionEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        // Top Bar & Search
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BIBLIOTECA & HISTÓRICO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.5.sp,
                            fontSize = 10.sp
                        ),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gravações Armazenadas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Clear All Button
                if (recordings.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(SophisticatedSurfaceVariant, CircleShape)
                            .border(1.dp, SophisticatedOutline, CircleShape)
                            .testTag("clear_all_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpar Todo o Histórico",
                            tint = ListeningCoral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Pesquisar nas transcrições, línguas, moradas...", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LavenderPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar busca", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LavenderPrimary,
                    unfocusedBorderColor = SophisticatedOutline,
                    focusedContainerColor = SophisticatedSurface,
                    unfocusedContainerColor = SophisticatedSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            FilterChipsRow(
                selectedLanguage = selectedLanguageFilter,
                showFavoritesOnly = showFavoritesOnly,
                onSelectLanguage = { viewModel.setLanguageFilter(it) },
                onToggleFavorites = { viewModel.toggleShowFavoritesOnly() }
            )
        }

        if (recordings.isEmpty()) {
            EmptyHistoryView(
                hasFilters = searchQuery.isNotBlank() || selectedLanguageFilter != null || showFavoritesOnly
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    items = recordings,
                    key = { it.id }
                ) { record ->
                    HistoryItemCard(
                        record = record,
                        playbackInfo = playbackInfo,
                        onPlayPause = { viewModel.playAudio(record) },
                        onSeek = { viewModel.seekAudio(it) },
                        onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(record) },
                        onEdit = { recordToEdit = record },
                        onDelete = { recordToDelete = record },
                        onSendSmtp = { recordToSendSmtp = record }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }

    // Delete Single Record Confirmation Dialog
    recordToDelete?.let { record ->
        AlertDialog(
            containerColor = SophisticatedSurface,
            onDismissRequest = { recordToDelete = null },
            title = { Text("Eliminar Registo?", color = TextPrimary) },
            text = { Text("Esta ação apagará a transcrição e o respetivo ficheiro de áudio da memória local.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        recordToDelete = null
                        Toast.makeText(context, "Registo eliminado com sucesso.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Eliminar", color = ListeningCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Clear All Records Dialog
    if (showClearAllDialog) {
        AlertDialog(
            containerColor = SophisticatedSurface,
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Limpar Todo o Histórico?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tem a certeza que deseja eliminar todas as gravações e transcrições guardadas? Esta ação é irreversível.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                        Toast.makeText(context, "Todo o histórico foi limpo.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Limpar Tudo", color = ListeningCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // SMTP Send Dialog
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

    // Edit Title/Notes Dialog
    recordToEdit?.let { record ->
        var editTitle by remember { mutableStateOf(record.title) }
        var editNotes by remember { mutableStateOf(record.notes) }

        AlertDialog(
            containerColor = SophisticatedSurface,
            onDismissRequest = { recordToEdit = null },
            title = { Text("Editar Informações", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título do registo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = SophisticatedOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notas adicionais") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = SophisticatedOutline
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateTitleAndNotes(record.id, editTitle, editNotes)
                        recordToEdit = null
                    }
                ) {
                    Text("Guardar", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToEdit = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FilterChipsRow(
    selectedLanguage: String?,
    showFavoritesOnly: Boolean,
    onSelectLanguage: (String?) -> Unit,
    onToggleFavorites: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Todos" Chip
        FilterChip(
            selected = selectedLanguage == null && !showFavoritesOnly,
            onClick = { onSelectLanguage(null) },
            label = { Text("Todos") },
            shape = RoundedCornerShape(14.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LavenderPrimary,
                selectedLabelColor = Color(0xFF381E72),
                containerColor = SophisticatedSurface,
                labelColor = TextTertiary
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedLanguage == null && !showFavoritesOnly,
                borderColor = SophisticatedOutline,
                selectedBorderColor = LavenderPrimary
            )
        )

        // Favorites Chip
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onToggleFavorites,
            label = { Text("⭐ Favoritos") },
            shape = RoundedCornerShape(14.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AmberGold,
                selectedLabelColor = Color(0xFF381E72),
                containerColor = SophisticatedSurface,
                labelColor = TextTertiary
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = showFavoritesOnly,
                borderColor = SophisticatedOutline,
                selectedBorderColor = AmberGold
            )
        )

        // Language chips
        SupportedLanguages.ALL.forEach { lang ->
            FilterChip(
                selected = selectedLanguage == lang.code,
                onClick = {
                    if (selectedLanguage == lang.code) {
                        onSelectLanguage(null)
                    } else {
                        onSelectLanguage(lang.code)
                    }
                },
                label = { Text("${lang.flag} ${lang.namePt.split(" ").first()}") },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LavenderPrimary,
                    selectedLabelColor = Color(0xFF381E72),
                    containerColor = SophisticatedSurface,
                    labelColor = TextTertiary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedLanguage == lang.code,
                    borderColor = SophisticatedOutline,
                    selectedBorderColor = LavenderPrimary
                )
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    record: TranscriptionEntity,
    playbackInfo: com.example.audio.PlaybackInfo,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendSmtp: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val formattedDate = remember(record.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("pt", "PT"))
        sdf.format(Date(record.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_card_${record.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = BorderStroke(
            1.dp,
            if (record.isFavorite) AmberGold.copy(alpha = 0.6f) else SophisticatedOutline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Send via SMTP
                    IconButton(
                        onClick = onSendSmtp,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("smtp_button_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Enviar por SMTP",
                            tint = LavenderPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (record.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (record.isFavorite) AmberGold else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Individual Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_record_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = ListeningCoral.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Timestamp & Duration
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (record.durationMs > 0) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Duração: ${formatDuration(record.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LavenderPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Location Badge if available
            record.locationAddress?.let { address ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, SophisticatedOutline.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        if (record.latitude != null && record.longitude != null) {
                            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${record.latitude},${record.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GlowLavender,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (record.latitude != null) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Ver Mapa",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Player Component (if audio exists)
            if (record.audioFilePath.isNotBlank() && File(record.audioFilePath).exists()) {
                AudioPlayerCard(
                    recordId = record.id,
                    playbackInfo = playbackInfo,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onSpeedChange = onSpeedChange
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transcript Snippet or Full Expanded View
            if (!isExpanded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isExpanded = true },
                    color = SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "\"${record.transcription}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            color = TextPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (record.translationPt.isNotBlank() && record.languageCode != "pt") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🇵🇹 ${record.translationPt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderPrimary.copy(alpha = 0.9f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // Expanded View with Tabs
                Column {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = LavenderPrimary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Original (${record.flagEmoji})", fontSize = 13.sp, color = if (selectedTab == 0) LavenderPrimary else TextSecondary) }
                        )
                        if (record.translationPt.isNotBlank()) {
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Português 🇵🇹", fontSize = 13.sp, color = if (selectedTab == 1) LavenderPrimary else TextSecondary) }
                            )
                        }
                        if (record.summary.isNotBlank()) {
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Resumo", fontSize = 13.sp, color = if (selectedTab == 2) LavenderPrimary else TextSecondary) }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            when (selectedTab) {
                                0 -> Text(
                                    text = "\"${record.transcription}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = TextPrimary
                                )
                                1 -> Text(
                                    text = "\"${record.translationPt}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = TextPrimary
                                )
                                2 -> Column {
                                    Text(
                                        text = record.summary,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 20.sp
                                        ),
                                        color = TextPrimary
                                    )
                                    if (record.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Notas: ${record.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer action buttons & Primary Action Row
            Spacer(modifier = Modifier.height(14.dp))

            // Primary Action Buttons Row: SMTP Email & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Send via SMTP Primary Button
                Button(
                    onClick = onSendSmtp,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderPrimary,
                        contentColor = DeepPurpleOnPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("smtp_action_button_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = DeepPurpleOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Enviar Email (SMTP)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleOnPrimary
                    )
                }

                // Delete Record Button
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ListeningCoral.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ListeningCoral
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("delete_record_button_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = ListeningCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Eliminar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ListeningCoral
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary expand/collapse & copy/share toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = if (isExpanded) "Recolher" else "Ver Completo",
                        style = MaterialTheme.typography.labelMedium,
                        color = LavenderPrimary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = LavenderPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val textToCopy = if (selectedTab == 1 && record.translationPt.isNotBlank()) {
                                record.translationPt
                            } else {
                                record.transcription
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Alma Dumbo Transcrição", textToCopy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
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
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    """
                                        [Alma Dumbo - ${record.detectedLanguage} ${record.flagEmoji}]
                                        ${record.transcription}
                                        
                                        ${if (record.translationPt.isNotBlank()) "Tradução:\n" + record.translationPt else ""}
                                        ${if (record.locationAddress != null) "\nLocalização: " + record.locationAddress else ""}
                                    """.trimIndent()
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partilhar Transcrição"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partilhar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(hasFilters: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SophisticatedSurface, CircleShape)
                .border(1.dp, SophisticatedOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = LavenderPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasFilters) "Nenhum resultado encontrado" else "Nenhuma gravação guardada",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasFilters) {
                "Tente alterar os termos de pesquisa ou remover os filtros de língua."
            } else {
                "Grave áudios no menu 'Gravar' ou use a transcrição em tempo real no menu 'Live' para guardar registos com áudio, mapa e envio SMTP."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
