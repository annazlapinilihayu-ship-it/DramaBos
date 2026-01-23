# 🎬 DramaBos

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

**DramaBos** adalah aplikasi streaming Android modern yang dikhususkan untuk menonton drama China (Short Dramas). Aplikasi ini dibangun sepenuhnya menggunakan **Jetpack Compose** dan mengagregasi konten dari berbagai sumber (Dramabox & Melolo) untuk memberikan pengalaman menonton yang mulus.

> *"Nonton drama China tanpa batas"*

## ✨ Fitur Utama

* **Multi-Source Aggregation**: Mengambil konten dari dua sumber utama (`Dramabox` dan `Melolo`) dengan kemampuan filtering.
* **Video Player Canggih**:
    * Dibangun di atas **Media3 / ExoPlayer**.
    * **Gesture Controls**: Geser vertikal kiri untuk kecerahan, kanan untuk volume.
    * **Double Tap**: Ketuk dua kali untuk *seek* (maju/mundur) 5 detik.
    * **Quality Selector**: Pilihan resolusi video otomatis atau manual (1080p, 720p, 480p).
    * **Fullscreen Mode**: Mode layar penuh yang imersif.
* **Pustaka Pribadi (Library)**:
    * **Riwayat Tontonan**: Melanjutkan episode terakhir secara otomatis (menyimpan posisi durasi).
    * **Favorit**: Simpan drama kesukaan Anda.
    * **Manajemen Data**: Fitur hapus item (select & delete) untuk riwayat dan favorit.
* **Pencarian Cerdas**: Pencarian global yang menggabungkan hasil dari semua sumber API.
* **UI Modern & Responsif**:
    * Hero Carousel untuk drama unggulan.
    * Mode Gelap (Dark Theme) sebagai default.
    * Desain adaptif menggunakan Material3.
* **System Check**: Pengecekan status server (Maintenance / API Down) secara *real-time*.

## 🛠️ Tech Stack & Library

Aplikasi ini dibangun menggunakan teknologi Android terbaru:

* **Bahasa**: [Kotlin](https://kotlinlang.org/) (v2.3.0)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material3)
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Networking**:
    * [Retrofit](https://square.github.io/retrofit/) - HTTP Client
    * [OkHttp](https://square.github.io/okhttp/) - Interceptor & Logging
    * [Gson](https://github.com/google/gson) - JSON Parsing
* **Async & Concurrency**: Kotlin Coroutines & Flow
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
* **Local Storage**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) (Menyimpan riwayat & favorit)
* **Media**: [AndroidX Media3 (ExoPlayer)](https://developer.android.com/media/media3)
* **Navigation**: Navigation Compose

## 📂 Struktur Proyek

```text
com.sonzaix.dramabos
├── data/                  # Layer Data
│   ├── DataLayer.kt       # Model Data (Response, Item, VideoData)
│   ├── DramaApiService    # Interface Retrofit
│   ├── DramaRepository    # Logika pengambilan data (API & Flow)
│   └── DramaDataStore     # Penyimpanan lokal (History/Favorite)
├── ui/                    # Layer UI (Composable)
│   ├── DramaAppUI.kt      # Semua layar (Home, Player, Library, Search)
│   └── DramaApp           # Entry point Composable & NavHost
├── viewmodel/             # State Management
│   ├── MainViewModel      # Status aplikasi (Maintenance/Down)
│   ├── PlayerViewModel    # Logika pemutar video
│   ├── SearchViewModel    # Logika pencarian
│   └── ...
└── MainActivity.kt        # Activity utama
```

## 🚀 Cara Menjalankan Project

### Prasyarat
* Android Studio (Versi terbaru, merekomendasikan Ladybug atau lebih baru karena penggunaan SDK 36).
* JDK 17 atau lebih baru.

### Langkah Instalasi
- Cari sendiri caranya
- Atau tanya AI sana
  
### Konfigurasi Build Github Action
Project ini menggunakan konfigurasi signing di `build.gradle.kts`. Jika Anda ingin membuild menggunakan Github Action, Anda perlu menyiapkan Environment Variables berikut:

* `SIGNING_KEY_STORE_PATH`
* `SIGNING_STORE_PASSWORD`
* `SIGNING_KEY_ALIAS`
* `SIGNING_KEY_PASSWORD`

## ⚠️ Disclaimer

Aplikasi ini adalah proyek hobi/edukasi. Konten video yang ditampilkan diambil dari sumber pihak ketiga (`dramabox` dan `melolo`). Pengembang tidak meng-host video apa pun di server sendiri. Ketersediaan konten bergantung pada status API sumber tersebut.

## 🤝 Kontribusi

Kontribusi selalu diterima! Jika Anda ingin menambahkan fitur atau memperbaiki bug:

1.  Fork repositori ini.
2.  Buat branch fitur baru (`git checkout -b fitur-keren`).
3.  Commit perubahan Anda (`git commit -m 'Menambahkan fitur keren'`).
4.  Push ke branch (`git push origin fitur-keren`).
5.  Buat Pull Request.

---
Developed with ❤️ by [**Sonzai X**](https://t.me/November2k)
---
Special thank to @yourealya for API
