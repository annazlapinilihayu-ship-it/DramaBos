package com.sonzaix.shortxrama.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonzaix.shortxrama.R
import com.sonzaix.shortxrama.ui.theme.TextWhite

@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splashTransition")
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val rotationDegrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF0A0A0A),
                        Color(0xFF050505)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.4f),
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(350f, 250f),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(
                        scaleX = pulse,
                        scaleY = pulse,
                        rotationZ = rotationDegrees * 0.1f
                    )
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        clip = true
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                radius = 80.dp.value
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "ShortXrama Logo",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = "ShortXrama",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Nonton Drama China Tanpa Batas",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(999.dp),
                        clip = false
                    )
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}
