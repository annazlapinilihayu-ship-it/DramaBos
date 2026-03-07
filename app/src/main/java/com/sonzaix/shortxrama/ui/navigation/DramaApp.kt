package com.sonzaix.shortxrama.ui.navigation

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sonzaix.shortxrama.data.AppSettings
import com.sonzaix.shortxrama.data.AppSettingsStore
import com.sonzaix.shortxrama.ui.components.BottomNavBar
import com.sonzaix.shortxrama.ui.screens.*
import com.sonzaix.shortxrama.ui.theme.DramaTheme
import com.sonzaix.shortxrama.ui.theme.TextGray
import com.sonzaix.shortxrama.ui.theme.TextWhite
import com.sonzaix.shortxrama.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun DramaApp(
    mainVM: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettings())

    DramaTheme(settings = settings) {
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(1400)
            showSplash = false
        }

        if (showSplash) {
            SplashScreen()
            return@DramaTheme
        }

        // Update checker
        var showUpdateDialog by remember { mutableStateOf(false) }
        var updateUrl by remember { mutableStateOf("") }
        var updateMessage by remember { mutableStateOf("") }
        var latestVersionName by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://raw.githubusercontent.com/sonzaiekkusu/SonzaiEkkusu/master/shortxrama_update.json")
                    .build()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val json = org.json.JSONObject(body)
                        val remoteVersionCode = json.optInt("versionCode", 0)
                        val currentVersionCode = context.packageManager
                            .getPackageInfo(context.packageName, 0).let {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else @Suppress("DEPRECATION") it.versionCode
                            }
                        if (remoteVersionCode > currentVersionCode) {
                            latestVersionName = json.optString("versionName", "")
                            updateMessage = json.optString("changelog", "Versi baru tersedia!")
                            updateUrl = json.optString("downloadUrl", "")
                            showUpdateDialog = true
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Update Tersedia", fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                },
                text = {
                    Column {
                        Text("Versi baru: $latestVersionName", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(updateMessage, color = TextGray, fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (updateUrl.isNotEmpty()) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, updateUrl.toUri()))
                            }
                            showUpdateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Update Sekarang", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Nanti", color = TextGray)
                    }
                }
            )
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in listOf("foryou", "new", "rank", "search", "library")

        val isMaintenance by mainVM.isMaintenance.collectAsState()

        if (isMaintenance) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "MAINTENANCE", fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                },
                text = { Text(text = "Aplikasi sedang dalam perbaikan (Maintenance) atau Server Down. Mohon kembali lagi nanti.", color = TextGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://t.me/November2k".toUri())
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Beritahu Developer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { (context as? Activity)?.finish() }) {
                        Text("Close / Keluar", color = TextWhite)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            )
        }

        Scaffold(
            bottomBar = { if (showBottomBar) BottomNavBar(navController) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "foryou",
                modifier = Modifier.padding(padding)
            ) {
                composable("foryou") { ForYouScreen(navController) }
                composable("new") { NewScreen(navController) }
                composable("rank") { RankScreen(navController) }
                composable("search") { SearchScreen(navController) }
                composable("library") { LibraryScreen(navController) }
                composable("settings") { SettingsScreen(navController) }

                composable(
                    route = "detail/{bookId}?bookName={bookName}&cover={cover}&intro={intro}&source={source}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType },
                        navArgument("bookName") { type = NavType.StringType; defaultValue = "" },
                        navArgument("cover") { type = NavType.StringType; defaultValue = "" },
                        navArgument("intro") { type = NavType.StringType; defaultValue = "" },
                        navArgument("source") { type = NavType.StringType; defaultValue = "melolo" }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    val bookName = backStackEntry.arguments?.getString("bookName") ?: ""
                    val cover = backStackEntry.arguments?.getString("cover") ?: ""
                    val intro = backStackEntry.arguments?.getString("intro") ?: ""
                    val source = backStackEntry.arguments?.getString("source") ?: "melolo"
                    DetailPlayerScreen(navController, bookId, source, bookName, cover, intro)
                }

                composable(
                    route = "player_full/{bookId}/{index}/{bookName}?source={source}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType },
                        navArgument("index") { type = NavType.IntType },
                        navArgument("bookName") { type = NavType.StringType },
                        navArgument("source") { type = NavType.StringType; defaultValue = "melolo" }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    val index = backStackEntry.arguments?.getInt("index") ?: 0
                    val bookName = backStackEntry.arguments?.getString("bookName") ?: ""
                    val source = backStackEntry.arguments?.getString("source") ?: "melolo"
                    PlayerScreen(navController, bookId, index, bookName, source)
                }
            }
        }
    }
}
