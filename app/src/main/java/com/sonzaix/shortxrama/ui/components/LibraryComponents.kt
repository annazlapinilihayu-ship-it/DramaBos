package com.sonzaix.shortxrama.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonzaix.shortxrama.data.FavoriteDrama
import com.sonzaix.shortxrama.data.LastWatched
import com.sonzaix.shortxrama.ui.theme.*
import com.sonzaix.shortxrama.ui.util.formatDuration
import java.net.URLDecoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableHistoryItem(
    item: LastWatched,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, uncheckedColor = TextGray)
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, Color.White.copy(0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(Modifier.padding(12.dp)) {
                Box(modifier = Modifier.width(90.dp).aspectRatio(3f/4f).clip(RoundedCornerShape(10.dp)).background(Color.Black)) {
                    if (!item.cover.isNullOrEmpty()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(item.cover).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center).size(32.dp))
                    }
                    if (item.position > 0) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color.Black.copy(0.8f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 3.dp)) {
                            Text(text = formatDuration(item.position), color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f).height(IntrinsicSize.Min), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        val decodedName = try { URLDecoder.decode(item.bookName, "UTF-8") } catch (_: Exception) { item.bookName }
                        Text(decodedName, maxLines = 2, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, overflow = TextOverflow.Ellipsis, color = TextWhite, lineHeight = 19.sp, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Episode ${item.chapterIndex + 1}", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            ProviderBadge(source = item.source, size = 16.dp)
                        }
                        val introText = item.introduction
                        if (!introText.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(introText, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 11.sp, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableFavoriteItem(
    item: FavoriteDrama,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, uncheckedColor = TextGray)
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, Color.White.copy(0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(Modifier.padding(12.dp)) {
                Box(modifier = Modifier.width(90.dp).aspectRatio(3f/4f).clip(RoundedCornerShape(10.dp)).background(Color.Black)) {
                    if (!item.cover.isNullOrEmpty()) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(item.cover).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center).size(32.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f).height(IntrinsicSize.Min), verticalArrangement = Arrangement.Center) {
                    Text(item.bookName, maxLines = 2, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, overflow = TextOverflow.Ellipsis, color = TextWhite, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.totalEpisodes > 0) {
                            Text("${item.totalEpisodes} Episode", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                        }
                        ProviderBadge(source = item.source, size = 16.dp)
                    }
                    val introText = item.introduction
                    if (!introText.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(introText, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 11.sp, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItemCard(item: FavoriteDrama, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, Color.White.copy(0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(3f/4f) 
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (!item.cover.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.cover)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f).height(IntrinsicSize.Min),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    item.bookName,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = TextWhite,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderBadge(source = item.source, size = 18.dp)

                    if (item.totalEpisodes > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${item.totalEpisodes} Episode",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
fun HistoryItemCard(item: LastWatched, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, Color.White.copy(0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(3f/4f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (!item.cover.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.cover)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Movie, null, tint = TextGray, modifier = Modifier.align(Alignment.Center))
                }

                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }

                if (item.position > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(item.position),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f).height(IntrinsicSize.Min),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val decodedName = try { URLDecoder.decode(item.bookName, "UTF-8") } catch (_: Exception) { item.bookName }
                    Text(
                        decodedName,
                        maxLines = 2,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        color = TextWhite,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProviderBadge(source = item.source, size = 16.dp)
                        Text(" • ", color = TextDarkGray, fontSize = 10.sp)
                        Text("Episode ${item.chapterIndex + 1}", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terakhir ditonton", fontSize = 10.sp, color = TextDarkGray)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0.6f },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray,
                    )
                }
            }
        }
    }
}
