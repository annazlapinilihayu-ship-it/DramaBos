package com.sonzaix.shortxrama.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonzaix.shortxrama.ui.theme.*

@Composable
fun FilterTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "tabBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else TextGray.copy(alpha = 0.6f),
        label = "tabBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextGray,
        label = "tabText"
    )
    val tabGradient = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    )
    val baseModifier = Modifier
        .clickable { onClick() }
        .shadow(if (isSelected) 8.dp else 0.dp, RoundedCornerShape(20.dp))
    val backgroundModifier = if (isSelected) {
        baseModifier.background(tabGradient, RoundedCornerShape(20.dp))
    } else {
        baseModifier.background(backgroundColor, RoundedCornerShape(20.dp))
    }
    Box(
        modifier = backgroundModifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SourceFilterRow(
    selectedSource: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SourceFilterChip("Melolo", "melolo", selectedSource.lowercase() == "melolo", MeloloColor) { onSelect("melolo") }
        SourceFilterChip("Dramabox", "dramabox", selectedSource.lowercase() == "dramabox", DramaboxColor) { onSelect("dramabox") }
        SourceFilterChip("ReelShort", "reelshort", selectedSource.lowercase() == "reelshort", ReelShortColor) { onSelect("reelshort") }
        SourceFilterChip("FreeReels", "freereels", selectedSource.lowercase() == "freereels", FreeReelsColor) { onSelect("freereels") }
        SourceFilterChip("NetShort", "netshort", selectedSource.lowercase() == "netshort", NetShortColor) { onSelect("netshort") }
        SourceFilterChip("MeloShort", "meloshort", selectedSource.lowercase() == "meloshort", MeloShortColor) { onSelect("meloshort") }
        SourceFilterChip("GoodShort", "goodshort", selectedSource.lowercase() == "goodshort", GoodShortColor) { onSelect("goodshort") }
        SourceFilterChip("DramaWave", "dramawave", selectedSource.lowercase() == "dramawave", DramaWaveColor) { onSelect("dramawave") }
    }
}

@Composable
fun SourceFilterChip(label: String, source: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(targetValue = if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant, label = "chipBg")
    val border by animateColorAsState(targetValue = if (selected) color else Color.White.copy(alpha = 0.12f), label = "chipBorder")
    val textColor by animateColorAsState(targetValue = if (selected) color else TextGray, label = "chipText")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .shadow(if (selected) 6.dp else 0.dp, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProviderBadge(source = source, size = 16.dp)
            Text(text = label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SourceFilterFab(
    selectedSource: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showLabel) {
        ExtendedFloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = modifier
        ) {
            Icon(Icons.Rounded.FilterList, null)
            Spacer(Modifier.width(8.dp))
            Text("Filter", fontWeight = FontWeight.SemiBold)
        }
    } else {
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = modifier
        ) {
            Icon(Icons.Rounded.FilterList, null)
        }
    }

    if (showDialog) {
        SourceFilterDialog(
            selectedSource = selectedSource,
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun SourceFilterDialog(
    selectedSource: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Sumber", color = TextWhite, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterOptionButton("Melolo", "melolo", selectedSource.lowercase() == "melolo", MeloloColor) { onSelect("melolo") }
                FilterOptionButton("DramaBox", "dramabox", selectedSource.lowercase() == "dramabox", DramaboxColor) { onSelect("dramabox") }
                FilterOptionButton("ReelShort", "reelshort", selectedSource.lowercase() == "reelshort", ReelShortColor) { onSelect("reelshort") }
                FilterOptionButton("FreeReels", "freereels", selectedSource.lowercase() == "freereels", FreeReelsColor) { onSelect("freereels") }
                FilterOptionButton("NetShort", "netshort", selectedSource.lowercase() == "netshort", NetShortColor) { onSelect("netshort") }
                FilterOptionButton("MeloShort", "meloshort", selectedSource.lowercase() == "meloshort", MeloShortColor) { onSelect("meloshort") }
                FilterOptionButton("GoodShort", "goodshort", selectedSource.lowercase() == "goodshort", GoodShortColor) { onSelect("goodshort") }
                FilterOptionButton("DramaWave", "dramawave", selectedSource.lowercase() == "dramawave", DramaWaveColor) { onSelect("dramawave") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = TextGray) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun FilterOptionButton(label: String, source: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (selected) color else Color.White.copy(0.12f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProviderBadge(source = source, size = 22.dp)
            Text(label, color = if (selected) color else TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
