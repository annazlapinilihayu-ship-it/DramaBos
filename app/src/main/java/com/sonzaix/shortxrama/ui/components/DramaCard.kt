package com.sonzaix.shortxrama.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonzaix.shortxrama.data.DramaItem
import com.sonzaix.shortxrama.ui.theme.*

@Composable
fun DramaCard(drama: DramaItem, onClick: () -> Unit) {
    val epCount = drama.chapterCount ?: 0
    val playCount = drama.playCount ?: ""
    val (sourceText, sourceColor) = sourceInfo(drama.source)

    Column(Modifier.clickable { onClick() }) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .aspectRatio(0.68f)
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(18.dp),
                    clip = false
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(drama.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(18.dp)
                        },
                    contentScale = ContentScale.Crop
                )

                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(0.3f),
                                    Color.Black.copy(0.95f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                val showEpBadge = epCount > 0
                val showPlayBadge = !showEpBadge && playCount.isNotEmpty()

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    ProviderBadge(source = drama.source, size = 18.dp)
                }

                if (showEpBadge || showPlayBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        BadgeBackground.copy(alpha = 0.9f),
                                        BadgeBackground.copy(alpha = 0.8f)
                                    )
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.width(IntrinsicSize.Max)
                        ) {
                            if (showPlayBadge) {
                                Icon(Icons.Rounded.Visibility, null, tint = TextWhite, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(playCount, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            } else {
                                Icon(Icons.Rounded.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("$epCount Eps", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = drama.bookName,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
