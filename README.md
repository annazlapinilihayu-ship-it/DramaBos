# 🎬 ShortXrama

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

**ShortXrama** adalah aplikasi streaming Android modern yang dikhususkan untuk menonton drama China (Short Dramas). Aplikasi ini dibangun sepenuhnya menggunakan **Jetpack Compose** dan mengagregasi konten dari berbagai sumber ternama untuk memberikan pengalaman menonton yang mulus dan lengkap.

> *"Nonton drama China tanpa batas dengan ShortXrama"*

## ✨ Fitur Utama

* **Multi-Source Aggregation**: Mengambil konten dari 8 sumber utama:
    * `Melolo`, `Dramabox`, `Reelshort`, `Freereels`, `Netshort`, `Meloshort`, `Goodshort`, dan `Dramawave`.
* **Video Player Canggih**:
    * Dibangun di atas **Media3 / ExoPlayer**.
    * **Subtitle Support**: Mendukung subtitle multi-bahasa (termasuk Bahasa Indonesia).
    * **Quality Selector**: Pilihan resolusi video (1080p, 720p, 480p) dengan dukungan codec (H264/H265).
    * **Gesture Controls**: Geser vertikal untuk kecerahan dan volume.
    * **Double Tap**: Ketuk dua kali untuk *seek* 5 detik.
    * **Fullscreen Mode**: Mode layar penuh yang imersif.
* **Fitur Tambahan**:
    * **Download**: Dukungan pengunduhan video untuk ditonton secara offline.
    * **Pustaka Pribadi**: Riwayat tontonan (menyimpan posisi durasi) dan Favorit.
    * **Pencarian Global**: Mencari drama dari semua sumber sekaligus.
* **UI Modern & Premium**:
    * Desain Material3 yang adaptif dan responsif.
    * Hero Carousel untuk konten unggulan.
    * Mode Gelap (Dark Theme) yang elegan.

## 🛠️ Tech Stack & Library

Aplikasi ini menggunakan teknologi Android terkini:

* **Bahasa**: [Kotlin](https://kotlinlang.org/) (v2.3.0)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material3)
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Networking**:
    * [Retrofit](https://square.github.io/retrofit/) (v3.0.0) - HTTP Client
    * [OkHttp](https://square.github.io/okhttp/) - Interceptor & Logging
* **Async & Concurrency**: Kotlin Coroutines & Flow
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
* **Media**: [AndroidX Media3](https://developer.android.com/media/media3)
* **Data Storage**: DataStore Preferences (History & Favorites)

## 📂 Struktur Proyek

```text
com.sonzaix.shortxrama
├── data/                  # Layer Data & Networking
│   ├── ApiModels.kt       # Model API (Response)
│   ├── AppModels.kt       # Model Data Domain
│   ├── DramaApiService    # Interface Retrofit
│   ├── DramaRepository    # Abstraksi Pengambilan Data
│   └── DramaDataStore     # Local Storage
├── ui/                    # Layer UI (Jetpack Compose)
│   ├── components/        # Komponen UI Reusable
│   ├── screens/           # Layar Aplikasi (Home, Player, Search, etc.)
│   ├── navigation/        # NavHost & Route
│   └── theme/             # Material3 Theme & Styles
├── viewmodel/             # State Management
└── ShortXramaApp.kt       # Application Class
```

## 🚀 Cara Menjalankan Project

### Prasyarat
* **Android Studio**: Versi Ladybug atau lebih baru (SDK 36).
* **JDK**: Versi 17 atau lebih baru.

### Instalasi
1. Clone repositori ini.
2. Buka proyek di Android Studio.
3. Tunggu proses sinkronisasi Gradle selesai.
4. Jalankan pada Emulator atau Perangkat Fisik.

## ⚠️ Disclaimer

Aplikasi ini adalah proyek hobi/edukasi. Konten video diambil dari API pihak ketiga. Pengembang tidak menyimpan konten video di server sendiri. Ketersediaan konten sepenuhnya bergantung pada status server sumber.

---
Developed with ❤️ by [**Sonzai X**](https://t.me/November2k)
---
Special thanks to @yourealya for API support.
