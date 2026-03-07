package com.sonzaix.shortxrama.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonzaix.shortxrama.ui.theme.TextGray
import com.sonzaix.shortxrama.ui.theme.TextWhite
import java.net.URLEncoder

fun openTelegramBotDownload(context: Context, dramaId: String, source: String, epStart: Int, epEnd: Int) {
    val encodedSource = URLEncoder.encode(source, "UTF-8")
    val botCommand = "/download ${dramaId} ${encodedSource} ${epStart} ${epEnd}"
    val botUsername = "ShortXramaBot"
    val url = "https://t.me/$botUsername?start=${URLEncoder.encode("dl_${dramaId}_${encodedSource}_${epStart}_${epEnd}", "UTF-8")}"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: Copy command to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Bot Command", botCommand))
    }
}

@Composable
fun DownloadEpisodeDialog(
    dramaId: String,
    dramaSource: String,
    dramaNa: String,
    totalEpisodes: Int,
    onDismiss: () -> Unit
) {
    var startEp by remember { mutableStateOf("1") }
    var endEp by remember { mutableStateOf(totalEpisodes.toString()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Unduh Episode", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(dramaNa, color = TextGray, fontSize = 13.sp, maxLines = 1)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pilih range episode untuk diunduh via Telegram Bot:", color = TextGray, fontSize = 13.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startEp,
                        onValueChange = { startEp = it.filter { c -> c.isDigit() } },
                        label = { Text("Dari Ep", color = TextGray, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(0.12f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                    Text("—", color = TextGray)
                    OutlinedTextField(
                        value = endEp,
                        onValueChange = { endEp = it.filter { c -> c.isDigit() } },
                        label = { Text("Sampai Ep", color = TextGray, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(0.12f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                }
                Text(
                    "Total: $totalEpisodes episode tersedia",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startEp.toIntOrNull() ?: 1
                    val e = endEp.toIntOrNull() ?: totalEpisodes
                    openTelegramBotDownload(context, dramaId, dramaSource, s.coerceAtLeast(1), e.coerceAtMost(totalEpisodes))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Unduh via Bot", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
