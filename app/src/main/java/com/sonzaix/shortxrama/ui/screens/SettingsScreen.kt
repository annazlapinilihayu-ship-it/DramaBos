package com.sonzaix.shortxrama.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonzaix.shortxrama.keepalive.KeepAliveScheduler
import com.sonzaix.shortxrama.keepalive.KeepAliveService
import com.sonzaix.shortxrama.data.*
import com.sonzaix.shortxrama.ui.components.*
import com.sonzaix.shortxrama.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())
    val dataStore = remember { DramaDataStore(context) }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = dataStore.exportBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
            Toast.makeText(context, "Backup disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            val ok = dataStore.importBackupJson(content)
            val msg = if (ok) "Backup diimpor" else "Gagal impor backup"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val keepAlivePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            KeepAliveService.start(context)
            KeepAliveScheduler.schedule(context)
        } else {
            scope.launch { settingsStore.setKeepAliveEnabled(false) }
            KeepAliveService.stop(context)
            KeepAliveScheduler.cancel(context)
            Toast.makeText(context, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
            }
            Text("Pengaturan", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tema UI Section Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSectionTitle("Tema UI")
                        SettingsToggleRow(
                            label = "Mode AMOLED",
                            description = "Background hitam pekat",
                            checked = settings.amoledMode,
                            onToggle = { scope.launch { settingsStore.setAmoledMode(it) } }
                        )
                        Spacer(Modifier.height(8.dp))
                        SettingsToggleRow(
                            label = "Sembunyikan Drama Ditonton",
                            description = "Hilangkan drama yang sudah ditonton dari beranda",
                            checked = settings.hideWatchedDramas,
                            onToggle = { scope.launch { settingsStore.setHideWatchedDramas(it) } }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Accent color", color = TextGray, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val options = listOf(
                                "Rose" to "#FF2965",
                                "Sky" to "#2D9CDB",
                                "Mint" to "#2ECC71",
                                "Amber" to "#F2C94C",
                                "Coral" to "#FF7A59"
                            )
                            options.forEach { (label, hex) ->
                                AccentColorChip(
                                    label = label,
                                    color = parseColorHex(hex, MaterialTheme.colorScheme.primary),
                                    selected = settings.accentColor.equals(hex, true),
                                    onClick = { scope.launch { settingsStore.setAccentColor(hex) } }
                                )
                            }
                        }
                    }
                }
            }

            // Background Section Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSectionTitle("Background")
                        SettingsToggleRow(
                            label = "Keep Alive",
                            description = """
                            • Menjaga app tetap aktif di latar belakang.
                            • Jika notifikasinya mengganggu, kamu bisa matikan di:
                            • App Info ShortXrama > Kategori Notifikasi > ShortXrama Keep Alive
                            • Dengan begitu Keep Alive tetap berjalan tanpa adanya notifikasi yang mengganggu.
                            """.trimIndent(),
                            checked = settings.keepAliveEnabled,
                            onToggle = { enabled ->
                                scope.launch { settingsStore.setKeepAliveEnabled(enabled) }
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            keepAlivePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@SettingsToggleRow
                                        }
                                    }
                                    KeepAliveService.start(context)
                                    KeepAliveScheduler.schedule(context)
                                } else {
                                    KeepAliveService.stop(context)
                                    KeepAliveScheduler.cancel(context)
                                }
                            }
                        )
                    }
                }
            }

            // Backup & Sync Section Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSectionTitle("Backup & Sync")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { exportLauncher.launch("shortxrama-backup.json") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Import", color = TextWhite)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Impor akan mengganti data lokal", color = TextDarkGray, fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                var showDonationDialog by remember { mutableStateOf(false) }
                Button(
                    onClick = { showDonationDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("DONASI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (showDonationDialog) {
                    AlertDialog(
                        onDismissRequest = { showDonationDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text("Donasi via QRIS", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data("https://raw.githubusercontent.com/sonzaiekkusu/SonzaiEkkusu/master/QRIS.jpeg")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "QRIS",
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("Scan QRIS di atas untuk donasi", color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        },
                        confirmButton = { TextButton(onClick = { showDonationDialog = false }) { Text("Tutup", color = MaterialTheme.colorScheme.primary) } }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = Color.White.copy(0.08f), thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))
                    Text("Developed by Sonzai X シ", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Streaming drama China tanpa batas", color = TextDarkGray, fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/November2k".toUri())) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color(0xFF0088CC), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Telegram", color = TextGray, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/November2kBio".toUri())) }) {
                            Icon(Icons.Filled.Notifications, null, tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Channel Update", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
