# Lumora Gallery 🌌

[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Design-Material%20Design%203-purple.svg?style=flat)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg?style=flat)](LICENSE)

An elegant, responsive, and privacy-first Android Gallery application crafted with precision using **Kotlin**, **Jetpack Compose**, and **Material Design 3 (M3)**.

Lumora Gallery is designed to provide a fast, secure, and visually breathtaking media browser experience with robust local privacy features and an intuitive creative suite.

---

## ✨ Features

### 1. Adaptive Media Stream & Timeline
* **Pinch-to-Zoom Density**: Instantly adjust grid densities from 2 to 5 columns on the fly.
* **Smart Section Headers**: View photos beautifully organized in chronological sections with responsive "Select All / Deselect All" actions when in selection mode.
* **Smart Filtering & Sorting**: Quickly filter by all files, photos, videos, or favorites. Sort by date added, name, or size.

### 2. Creative Studio 🎨
Transform your moments into digital art with built-in creative features:
* **Premium Collage Maker**: Combine 2 to 9 photos into beautiful compositions. Adjust margins (item spacing) and border corners in real-time. Choose from **6 prebuilt, elegant templates**:
  * **Grid**: Classic symmetric grids.
  * **Vertical**: Balanced vertical columns.
  * **Horizontal**: Wide horizontal lanes.
  * **Featured Focus**: Elegant magazine style with your main image prominent (60% width) and supporting photos stacked on the side.
  * **Cinematic Split**: Splits photos into upper and lower cinematic sections.
  * **Asymmetric Masonry**: A smart, self-adjusting grid that customizes layouts based on the number of photos selected.
* **Fullscreen Slideshow**: Tap into custom slideshows with premium transitions.
* **Memory Video & Motion Photo**: Browse live items and manage clips seamlessly.

### 3. Secure Vault & Privacy Shield 🔒
Your privacy is our utmost priority:
* **Fully Encrypted Vault**: Protect sensitive photos/videos behind a Secure PIN lock.
* **PBKDF2 & AES-GCM Encryption**: Secure files with cryptographic keys derived locally using high-performance PBKDF2 SHA-256. Plaintext PINs or passwords are never stored.
* **Offline Security**: All cryptographic processes occur entirely on-device with zero server synchronization.

### 4. Trash & Recycle Manager ♻️
* **Safe Deletion**: Accidantally deleted a photo? Lumora moves files to a secure local trash can.
* **30-Day Recovery**: Easily review and restore items to their original folders or permanently erase them instantly.

---

## 🏗️ Architecture & Stack

Lumora is built on modern Android development practices adhering to **Clean Architecture** and **MVVM** design principles:

* **Presentation Layer**: Jetpack Compose (M3) utilizing a unified `Theme.kt` with dynamic color palettes and responsive layouts.
* **Business Logic & State**: Kotlin Coroutines & StateFlow coupled with Jetpack `ViewModel`.
* **Data Layer & Persistence**: **Room Database** for high-speed local data persistence, and SQLite integration.
* **Image Loading & Rendering**: **Coil** with fully asynchronous multi-thread thread-safe image request configurations.
* **Security & Crypto**: PBKDF2 with standard Java Cryptography Architecture (`javax.crypto`).

---

## 🚀 Build Instructions

### Prerequisites
* **Android Studio Ladybug** (or newer)
* **JDK 17** or newer installed
* Android Device/Emulator running **Android 8.0 (API 26)** or higher

### Step-by-Step Compilation
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/lumora-gallery.git
   cd lumora-gallery
   ```
2. **Import to Android Studio**:
   * Open Android Studio, click **File > Open**, and select the cloned root folder.
3. **Build APK**:
   * Use the Gradle toolbar or build via CLI:
     ```bash
     ./gradlew assembleDebug
     ```
   * The compiled `.apk` will be generated in: `/app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
