package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.LanguageLearningData
import com.example.audio.LanguageMeta
import com.example.audio.LanguageStudyPack
import com.example.audio.PhraseItem
import com.example.audio.SupportedLanguages
import com.example.audio.VocabularyItem
import com.example.ui.components.ScriptTag
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.EmeraldSuccess
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
import com.example.ui.viewmodel.TranscriberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagesCatalogScreen(
    viewModel: TranscriberViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ttsManager = viewModel.ttsManager
    val isTtsSpeaking by ttsManager.isSpeaking.collectAsState()
    val isTtsReady by ttsManager.isInitialized.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageForPractice by remember { mutableStateOf<LanguageMeta?>(null) }
    var selectedFamilyFilter by remember { mutableStateOf<String?>(null) }

    val families = remember {
        listOf("Todas") + SupportedLanguages.ALL.map { it.family }.distinct().sorted()
    }

    val filteredLanguages = remember(searchQuery, selectedFamilyFilter) {
        var list = SupportedLanguages.ALL
        if (!selectedFamilyFilter.isNullOrBlank() && selectedFamilyFilter != "Todas") {
            list = list.filter { it.family == selectedFamilyFilter }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase().trim()
            list = list.filter {
                it.namePt.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.region.lowercase().contains(q) ||
                it.script.lowercase().contains(q) ||
                it.family.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
        list
    }

    // Modal Bottom Sheet for Practice (20 Words, 20 Phrases, TTS Input)
    selectedLanguageForPractice?.let { lang ->
        LanguagePracticeBottomSheet(
            language = lang,
            viewModel = viewModel,
            onDismiss = {
                ttsManager.stop()
                selectedLanguageForPractice = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CATÁLOGO & ESTÚDIO DE VOZ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.5.sp,
                            fontSize = 10.sp
                        ),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Línguas & Pronúncia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // TTS ready badge
                Surface(
                    color = if (isTtsReady) EmeraldSuccess.copy(alpha = 0.15f) else AmberGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isTtsReady) EmeraldSuccess.copy(alpha = 0.5f) else AmberGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isTtsReady) EmeraldSuccess else AmberGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isTtsReady) "Voz TTS Ativa" else "A preparar Voz",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isTtsReady) EmeraldSuccess else AmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar por língua, dialeto, país ou escrita...", color = TextSecondary, fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LavenderPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input"),
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

            // Family filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(families) { fam ->
                    val isSelected = (selectedFamilyFilter == fam) || (fam == "Todas" && selectedFamilyFilter == null)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) LavenderContainer else SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) LavenderPrimary else SophisticatedOutline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.clickable {
                            selectedFamilyFilter = if (fam == "Todas") null else fam
                        }
                    ) {
                        Text(
                            text = fam,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (isSelected) LavenderOnContainer else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Languages List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = filteredLanguages,
                key = { it.code }
            ) { lang ->
                LanguageCatalogCard(
                    lang = lang,
                    onPlaySample = {
                        ttsManager.speak(lang.sampleGreeting, lang.code)
                        Toast.makeText(context, "A reproduzir saudação em ${lang.namePt}...", Toast.LENGTH_SHORT).show()
                    },
                    onOpenPractice = {
                        selectedLanguageForPractice = lang
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LanguageCatalogCard(
    lang: LanguageMeta,
    onPlaySample: () -> Unit,
    onOpenPractice: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lang_catalog_card_${lang.code}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = BorderStroke(
            1.dp,
            SophisticatedOutline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row with Flag & Names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = lang.flag, fontSize = 28.sp)
                    Column {
                        Text(
                            text = lang.namePt,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = lang.nativeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                ScriptTag(scriptName = lang.script)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Information Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📍 ${lang.region}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "🏷️ ${lang.family}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Exemplo: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"${lang.sampleGreeting}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif
                                ),
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Instant sample audio button
                        IconButton(
                            onClick = onPlaySample,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Ouvir saudação",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Practice & Voice Studio Action Button
            Button(
                onClick = onOpenPractice,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LavenderContainer,
                    contentColor = LavenderOnContainer
                ),
                border = BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("open_practice_button_${lang.code}")
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = LavenderPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "20 Palavras • 20 Frases • Áudio TTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = LavenderOnContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePracticeBottomSheet(
    language: LanguageMeta,
    viewModel: TranscriberViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ttsManager = viewModel.ttsManager
    val isSpeaking by ttsManager.isSpeaking.collectAsState()
    val isTtsReady by ttsManager.isInitialized.collectAsState()

    val studyPack = remember(language.code) {
        LanguageLearningData.getStudyPack(language.code)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var customText by remember { mutableStateOf("") }
    var currentPlayingItem by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SophisticatedSurface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header with language banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = language.flag, fontSize = 32.sp)
                    Column {
                        Text(
                            text = language.namePt,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${language.nativeName} • ${language.script}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LavenderPrimary
                        )
                    }
                }

                if (isSpeaking) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ListeningCoralContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, ListeningCoral)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { ttsManager.stop() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Parar Áudio",
                                tint = ListeningCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "A falar...",
                                style = MaterialTheme.typography.labelSmall,
                                color = ListeningCoral,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs (20 Palavras, 20 Frases, Texto para Áudio)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                contentColor = LavenderPrimary,
                indicator = {},
                divider = {}
            ) {
                listOf(
                    0 to "📖 20 Palavras",
                    1 to "💬 20 Frases",
                    2 to "✍️ Texto p/ Voz"
                ).forEach { (idx, title) ->
                    val isTabSelected = selectedTab == idx
                    Tab(
                        selected = isTabSelected,
                        onClick = {
                            ttsManager.stop()
                            selectedTab = idx
                        },
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTabSelected) LavenderPrimary else Color.Transparent)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTabSelected) DeepPurpleOnPrimary else TextSecondary,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // 20 Words List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(studyPack.words) { index, item ->
                                VocabularyListItem(
                                    index = index + 1,
                                    item = item,
                                    langCode = language.code,
                                    isPlaying = currentPlayingItem == item.word && isSpeaking,
                                    onPlay = {
                                        currentPlayingItem = item.word
                                        ttsManager.speak(item.word, language.code)
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        // 20 Phrases List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(studyPack.phrases) { index, phrase ->
                                PhraseListItem(
                                    index = index + 1,
                                    phrase = phrase,
                                    langCode = language.code,
                                    isPlaying = currentPlayingItem == phrase.phrase && isSpeaking,
                                    onPlay = {
                                        currentPlayingItem = phrase.phrase
                                        ttsManager.speak(phrase.phrase, language.code)
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Custom Text-to-Speech (Texto para Áudio) Input
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ÁREA DE TEXTO LIVRE PARA ÁUDIO:",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                color = TextTertiary,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                placeholder = {
                                    Text(
                                        "Escreva qualquer palavra, texto ou frase em ${language.namePt} para converter em áudio e escutar a pronúncia natural...",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .testTag("tts_custom_text_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LavenderPrimary,
                                    unfocusedBorderColor = SophisticatedOutline,
                                    focusedContainerColor = SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = SophisticatedSurfaceVariant.copy(alpha = 0.5f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            // Quick Suggestions Row
                            Text(
                                text = "Sugestões Rápidas:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val suggestions = listOf(
                                    language.sampleGreeting,
                                    studyPack.words.firstOrNull()?.word ?: "Olá",
                                    studyPack.phrases.firstOrNull()?.phrase ?: "Bom dia",
                                    studyPack.phrases.getOrNull(3)?.phrase ?: "Muito prazer"
                                )
                                items(suggestions) { text ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SophisticatedSurfaceVariant,
                                        border = BorderStroke(1.dp, SophisticatedOutline),
                                        modifier = Modifier.clickable { customText = text }
                                    ) {
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LavenderPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Play Audio Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (customText.isNotBlank()) {
                                            ttsManager.speak(customText.trim(), language.code)
                                            Toast.makeText(context, "A reproduzir áudio em ${language.namePt}...", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Por favor escreva algum texto primeiro.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LavenderPrimary,
                                        contentColor = DeepPurpleOnPrimary
                                    ),
                                    enabled = customText.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("tts_play_custom_audio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = DeepPurpleOnPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reproduzir Áudio",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                if (isSpeaking) {
                                    OutlinedButton(
                                        onClick = { ttsManager.stop() },
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, ListeningCoral),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Parar",
                                            tint = ListeningCoral
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Parar", color = ListeningCoral)
                                    }
                                }

                                IconButton(
                                    onClick = { customText = "" },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SophisticatedSurfaceVariant)
                                        .border(1.dp, SophisticatedOutline, RoundedCornerShape(16.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpar texto",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabularyListItem(
    index: Int,
    item: VocabularyItem,
    langCode: String,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (isPlaying) GlowLavender else SophisticatedOutline.copy(alpha = 0.5f),
        label = "item_border"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isPlaying) LavenderContainer.copy(alpha = 0.25f) else SophisticatedSurfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
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
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) LavenderPrimary else SophisticatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) DeepPurpleOnPrimary else TextSecondary
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) LavenderPrimary else TextPrimary
                        )

                        if (item.transcription.isNotBlank()) {
                            Text(
                                text = "[${item.transcription}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                    }

                    Text(
                        text = item.meaningPt,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                    contentDescription = "Ouvir pronúncia",
                    tint = if (isPlaying) GlowLavender else LavenderPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PhraseListItem(
    index: Int,
    phrase: PhraseItem,
    langCode: String,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (isPlaying) GlowLavender else SophisticatedOutline.copy(alpha = 0.5f),
        label = "phrase_border"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isPlaying) LavenderContainer.copy(alpha = 0.25f) else SophisticatedSurfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) LavenderPrimary else SophisticatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) DeepPurpleOnPrimary else TextSecondary
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "\"${phrase.phrase}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isPlaying) LavenderPrimary else TextPrimary
                    )

                    Text(
                        text = "🇵🇹 ${phrase.translationPt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                    contentDescription = "Ouvir frase",
                    tint = if (isPlaying) GlowLavender else LavenderPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
