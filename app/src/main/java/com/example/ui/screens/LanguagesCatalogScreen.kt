package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    val isTtsReady by ttsManager.isInitialized.collectAsState()
    val enabledLangs by viewModel.enabledDumboLanguages.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguageDetail by remember { mutableStateOf<LanguageMeta?>(null) }
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

    // Modal Bottom Sheet with Complete Language Details and Studio
    selectedLanguageDetail?.let { lang ->
        val isEnabled = enabledLangs.contains(lang.code)
        LanguageFullDetailBottomSheet(
            language = lang,
            isEnabledInDumbo = isEnabled,
            onToggleEnabled = { enabled ->
                viewModel.toggleDumboLanguage(lang.code, enabled)
            },
            viewModel = viewModel,
            onDismiss = {
                ttsManager.stop()
                selectedLanguageDetail = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        // Top Header
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
                        text = "CATÁLOGO DE LÍNGUAS",
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

                // Active languages summary badge
                Surface(
                    color = LavenderContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${enabledLangs.size}/${SupportedLanguages.ALL.size} Ativas",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar língua, país ou escrita...", color = TextSecondary, fontSize = 13.5.sp) },
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

            Spacer(modifier = Modifier.height(8.dp))

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

        // Clean Simple List View
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = filteredLanguages,
                key = { it.code }
            ) { lang ->
                val isEnabled = enabledLangs.contains(lang.code)
                SimpleLanguageListItem(
                    lang = lang,
                    isEnabledInDumbo = isEnabled,
                    onPlaySample = {
                        ttsManager.speak(lang.sampleGreeting, lang.code)
                        Toast.makeText(context, "A reproduzir saudação em ${lang.namePt}...", Toast.LENGTH_SHORT).show()
                    },
                    onOpenDetail = {
                        selectedLanguageDetail = lang
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
private fun SimpleLanguageListItem(
    lang: LanguageMeta,
    isEnabledInDumbo: Boolean,
    onPlaySample: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenDetail() }
            .testTag("lang_item_${lang.code}"),
        color = SophisticatedSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isEnabledInDumbo) GlowLavender.copy(alpha = 0.4f) else SophisticatedOutline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flag and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SophisticatedSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = lang.flag, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = lang.namePt,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (isEnabledInDumbo) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldSuccess.copy(alpha = 0.18f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "DUMBO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = lang.nativeName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = LavenderPrimary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextTertiary
                        )
                        Text(
                            text = lang.region,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action Icons (Play Sample + Arrow to detail)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onPlaySample,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Ouvir saudação",
                        tint = LavenderPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Ver detalhes",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageFullDetailBottomSheet(
    language: LanguageMeta,
    isEnabledInDumbo: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SophisticatedBackground,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp),
                shape = CircleShape,
                color = SophisticatedOutline
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = language.flag, fontSize = 38.sp)
                    Column {
                        Text(
                            text = language.namePt,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.titleMedium,
                            color = LavenderPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                ScriptTag(scriptName = language.script)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dumbo Toggle Switch inside Detail
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isEnabledInDumbo) LavenderContainer.copy(alpha = 0.5f) else SophisticatedSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (isEnabledInDumbo) LavenderPrimary else SophisticatedOutline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ativa no Dumbo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Usar em Gravar, Escutar e Diálogo automático",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isEnabledInDumbo,
                        onCheckedChange = { onToggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LavenderPrimary,
                            checkedTrackColor = LavenderContainer,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SophisticatedSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Complete Linguistic Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SophisticatedSurface,
                border = BorderStroke(1.dp, SophisticatedOutline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DADOS LINGUÍSTICOS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Código ISO / BCP-47:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = language.code, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Família Linguística:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = language.family, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sistema de Escrita:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = language.script, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Região Principal:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = language.region, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Studio Tabs (20 Palavras, 20 Frases, Prática Livre)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SophisticatedSurfaceVariant,
                contentColor = LavenderPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = LavenderPrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("20 Palavras", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("20 Frases", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Estúdio de Voz", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // 20 VOCABULARY WORDS
                    studyPack.words.forEachIndexed { index, vocab ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.5f))
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
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = TextTertiary,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = vocab.word,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = vocab.meaningPt,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                            color = LavenderPrimary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        currentPlayingItem = vocab.word
                                        ttsManager.speak(vocab.word, language.code)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir palavra",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // 20 PHRASES
                    studyPack.phrases.forEachIndexed { index, phrase ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = SophisticatedSurfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, SophisticatedOutline.copy(alpha = 0.5f))
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
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = TextTertiary,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = phrase.phrase,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = phrase.translationPt,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = AmberGold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        currentPlayingItem = phrase.phrase
                                        ttsManager.speak(phrase.phrase, language.code)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Ouvir frase",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // CUSTOM TTS VOICE STUDIO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                        border = BorderStroke(1.dp, GlowLavender.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Escreva qualquer texto em ${language.namePt}:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                placeholder = { Text("Exemplo: ${language.sampleGreeting}", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LavenderPrimary,
                                    unfocusedBorderColor = SophisticatedOutline,
                                    focusedContainerColor = SophisticatedSurfaceVariant,
                                    unfocusedContainerColor = SophisticatedSurfaceVariant,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val textToSpeak = customText.ifBlank { language.sampleGreeting }
                                    ttsManager.speak(textToSpeak, language.code)
                                    Toast.makeText(context, "A falar em ${language.namePt}...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LavenderPrimary,
                                    contentColor = DeepPurpleOnPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = DeepPurpleOnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ouvir Pronúncia Nativa",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleOnPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
