package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LavenderContainer
import com.example.ui.theme.LavenderOnContainer
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SophisticatedOutline
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LanguageBadge(
    flagEmoji: String,
    languageName: String,
    confidence: Float? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = LavenderContainer,
    contentColor: Color = LavenderOnContainer
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Detected Chip (#4A4458 container)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = flagEmoji,
                fontSize = 15.sp
            )
            Text(
                text = "Detected:",
                style = MaterialTheme.typography.labelSmall,
                color = LavenderOnContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = languageName,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Confidence Chip (#2B2930 container with #49454F border)
        if (confidence != null && confidence > 0f) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SophisticatedSurfaceVariant)
                    .border(1.dp, SophisticatedOutline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confidence ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ScriptTag(
    scriptName: String,
    modifier: Modifier = Modifier
) {
    if (scriptName.isBlank()) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SophisticatedSurfaceVariant)
            .border(1.dp, SophisticatedOutline, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = scriptName,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontWeight = FontWeight.Medium
        )
    }
}
