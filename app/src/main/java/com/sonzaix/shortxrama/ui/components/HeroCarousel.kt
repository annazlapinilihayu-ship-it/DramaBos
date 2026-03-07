package com.sonzaix.shortxrama.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sonzaix.shortxrama.ui.theme.TextGray
import com.sonzaix.shortxrama.ui.theme.TextWhite

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    featuredItems: List<DramaItem>,
    onClick: (DramaItem) -> Unit
) {
    if (featuredItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featuredItems.size })

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            val nextPage = (pagerState.currentPage + 1) % featuredItems.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().height(380.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            pageSpacing = 10.dp,
            flingBehavior = PagerDefaults.flingBehavior(pagerState)
        ) { page ->
            val item = featuredItems[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(14.dp, RoundedCornerShape(20.dp))
                    .clickable { onClick(item) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.bookName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(20.dp)
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                                ),
                                startY = 160f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    val count = item.chapterCount ?: 0
                    if (count > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "•  $count Episode",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = item.bookName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 24.sp,
                        letterSpacing = 0.5.sp
                    )

                    if (!item.introduction.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = item.introduction,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { onClick(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
                        modifier = Modifier
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(28.dp),
                                clip = false
                            )
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Mulai Tonton", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val width = if (isSelected) 28.dp else 8.dp
                val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .height(6.dp)
                        .width(width)
                        .animateContentSize()
                )
            }
        }
    }
}
