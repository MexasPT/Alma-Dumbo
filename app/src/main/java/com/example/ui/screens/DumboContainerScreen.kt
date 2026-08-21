package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleOnPrimary
import com.example.ui.theme.GlowLavender
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SophisticatedBackground
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TranscriberViewModel

sealed class DumboSubTab(val index: Int, val title: String, val icon: ImageVector) {
    object Gravar : DumboSubTab(0, "Gravar", Icons.Default.Mic)
    object Escuta : DumboSubTab(1, "Escuta", Icons.Default.GraphicEq)
    object Dialogo : DumboSubTab(2, "Diálogo", Icons.Default.RecordVoiceOver)
}

@Composable
fun DumboContainerScreen(
    viewModel: TranscriberViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(DumboSubTab.Gravar, DumboSubTab.Escuta, DumboSubTab.Dialogo)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBackground)
    ) {
        // Top Sub-Tabs Navigation Bar
        TabRow(
            selectedTabIndex = selectedSubTabIndex,
            containerColor = SophisticatedSurface,
            contentColor = LavenderPrimary,
            divider = {
                androidx.compose.material3.HorizontalDivider(
                    thickness = 1.dp,
                    color = SophisticatedOutline.copy(alpha = 0.6f)
                )
            },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTabIndex]),
                    height = 3.dp,
                    color = LavenderPrimary
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedSubTabIndex == tab.index
                Tab(
                    selected = isSelected,
                    onClick = { selectedSubTabIndex = tab.index },
                    modifier = Modifier.testTag("dumbo_subtab_${tab.title.lowercase()}"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) GlowLavender else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                )
            }
        }

        // Sub-Tab Content
        Crossfade(
            targetState = selectedSubTabIndex,
            label = "dumboSubTabFade",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { tabIndex ->
            when (tabIndex) {
                0 -> RecordScreen(viewModel = viewModel)
                1 -> LiveScreen(viewModel = viewModel)
                2 -> DialogueScreen(viewModel = viewModel)
            }
        }
    }
}
