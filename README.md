# MacroBite 🍽️

A minimalist, privacy-first Android macro and calorie tracker built with 100% Jetpack Compose and Room.

**Zero ads. Zero subscriptions. Works 100% offline.**

[![Latest Release](https://img.shields.io/github/v/release/VamsiReddyBora/MacroBite?color=blue&label=Download%20APK)](https://github.com/VamsiReddyBora/MacroBite/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg?style=flat&logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg?style=flat)](LICENSE)

---

## 📥 Download

Get the pre-compiled Android release APK directly:

👉 **[Download Latest MacroBite.apk (v1.1.0)](https://github.com/VamsiReddyBora/MacroBite/releases/download/v1.1.0/MacroBite.apk)**

Or visit the **[Releases Tab](https://github.com/VamsiReddyBora/MacroBite/releases)** to view all versions and changelogs.

---

## ⚡ Core Features

- **Macro Gauge:** Segmented circular calorie ring (`/ 3,000 kcal`) with live progress bars for **Protein**, **Carbs**, and **Fats**.
- **Multimodal Food Logging:** Log in seconds via natural language text (*"2 eggs and butter toast"*), speech-to-text with ripple feedback, camera photo capture, or 1-tap hostel Quick Presets.
- **Barcode Scanner with Custom Barcode Memory:** Scans OpenFoodFacts barcodes. If an unknown product isn't found, snap its back-of-pack nutrition table once — MacroBite memorizes it locally and scans it instantly offline forever.
- **Static Home Screen Chat & Voice Widget:** Type or speak meals directly from your home screen with glowing theme borders and zero screen flashing — logs silently in the background.
- **Personal AI Assistant ("Raaya") & Device Copilot:** Atwater macro calculations and coaching, plus hands-free device actions: *"Call Mom"*, *"Set timer for 12 mins"*, *"WhatsApp Rahul"*, *"Open YouTube"*, *"Navigate to gym"*.
- **Contact Aliases:** Map voice nicknames (e.g., `"dad" ➔ "Daddy"`) for instant phone dialing without disambiguation prompts.
- **Smart Notifications & In-App HUD:** AI Well-Wisher meal reminders via exact Android alarms, floating spring HUD capsules, and a dedicated In-App Notification toggle in Settings.
- **Google Health Connect & Body Tracker:** Synchronize daily steps and active calorie expenditure. Track body weight with trend delta and calculate BMI.
- **Weekly Analytics & CSV Export:** 7-day intake chart against your target threshold and 1-tap CSV export for Google Sheets or Excel.
- **6 Dynamic Theme Accents:** Amber Gold, Emerald Green, Cobalt Blue, Crimson Red, Amethyst Purple, and Slate Gray in Dark and Light modes.
- **100% Privacy-First:** No analytics, no ads, and zero third-party telemetry. All data stays strictly on your device in a local SQLite database.

---

## 🚀 Build from Source

```bash
git clone https://github.com/VamsiReddyBora/MacroBite.git
cd MacroBite

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew testReleaseUnitTest
```
The compiled APK will be at `app/build/outputs/apk/release/MacroBite.apk`.

---

## 🔑 AI Setup (Optional)

MacroBite works 100% offline using its built-in local food dictionary.

To enable multimodal photo scanning and conversational AI advice:
1. Get a free API key at [Google AI Studio](https://aistudio.google.com/).
2. Open **MacroBite ➔ Settings ➔ Gemini AI Settings**.
3. Paste your key and tap **Save Key**.

---

## 🛠 Tech Stack

- **UI:** 100% Jetpack Compose (Material Design 3)
- **Architecture:** Clean Architecture + MVVM + UDF
- **Database:** Room Database 2.6.1 & Jetpack DataStore Preferences
- **Dependency Injection:** Dagger Hilt 2.50
- **Asynchronous:** Kotlin Coroutines & StateFlow
- **AI & Vision:** Google Generative AI SDK (`gemini-2.5-flash`, `gemini-3.5-flash-lite`) & ML Kit Barcode Scanning
- **Health:** Android Health Connect Client SDK 1.1.0

---

## 👨‍💻 Developer

**Vamsi Reddy Bora**
- **GitHub:** [@VamsiReddyBora](https://github.com/VamsiReddyBora)
- **Email:** [vamsireddy2534@gmail.com](mailto:vamsireddy2534@gmail.com)

---

## 📄 License

Licensed under the [MIT License](LICENSE).
