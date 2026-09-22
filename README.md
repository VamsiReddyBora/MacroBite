# MacroBite 🍽️

A minimalist, privacy-first Android macro and calorie tracker built with 100% Jetpack Compose and Room.

**Zero ads. Zero subscriptions. Works both online and offline.**

[![Latest Release](https://img.shields.io/github/v/release/VamsiReddyBora/MacroBite?color=blue&label=Download%20APK)](https://github.com/VamsiReddyBora/MacroBite/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg?style=flat&logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg?style=flat)](LICENSE)

---

## 📥 Download

Get the pre-compiled Android release APK directly:

👉 **[Download Latest MacroBite.apk (v1.3.0)](https://github.com/VamsiReddyBora/MacroBite/releases/download/v1.3.0/MacroBite.apk)**

Or visit the **[Releases Tab](https://github.com/VamsiReddyBora/MacroBite/releases)** to view all versions and changelogs.

---

## ⚡ Core Features

| Feature | Capabilities | Key Advantage |
| :--- | :--- | :--- |
| 🎯 **Macro Gauge** | Segmented calorie ring (`/ 3,000 kcal`) + Protein, Carbs & Fats bars | Instant visual feedback on remaining macros |
| 🎙️ **Multimodal Logging** | Plain text (*"2 eggs + toast"*), voice mic ripple, camera photos & 1-tap presets | Log any meal in 2–3 seconds |
| 🏷️ **Barcode Memory** | OpenFoodFacts scanner + photo fallback for unlisted packages | Snapping an unknown label saves the barcode locally forever |
| 🌐 **Custom API & Self-Hosting** | OpenAI-compatible custom Base URL (Tailscale, Local, Ollama, vLLM) + Google Gemini | Full control: self-host your own LLM backend or use Google AI |
| 📊 **Live Quota & Auto-Backup** | Live Gemini API quota probe + fully automatic silent local backup & restore | Seamless data persistence across installs with 1-tap restore |
| 📲 **Home Screen Widget** | Static home bar for background text & voice logging | Zero app launch, zero screen flash, silent background logging |
| 🤖 **AI Assistant & Copilot** | Atwater nutrition calculations + hands-free actions (*Calls, Timers, WhatsApp, Maps*) | Nutrition intelligence + device automation in one |
| 👥 **Contact Aliases** | Nickname mapping (e.g., `"dad"` ➔ `"Daddy"`) | Direct voice dialing without disambiguation prompts |
| 🔔 **Smart Alerts & HUD** | AI Well-Wisher meal alarms + spring-animated floating capsules | Exact Android alarm timing + toggleable in-app HUD |
| 👟 **Health & Analytics** | Health Connect sync (steps & burn), weight trends, BMI & CSV export | Unified offline fitness tracking & export to Excel |
| 🎨 **6 Theme Accents** | Amber Gold, Emerald, Cobalt, Crimson, Amethyst & Slate | Tailored Material 3 Dark & Light modes |
| 🔒 **100% Privacy-First** | Local SQLite Room DB + encrypted Jetpack DataStore | Zero ads, zero trackers, zero telemetry |

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

### Option A: Google Gemini API (Free)
1. Get a free API key at [Google AI Studio](https://aistudio.google.com/).
2. Open **MacroBite ➔ Settings ➔ AI Settings**.
3. Paste your key and tap **Save Key**.

### Option B: Custom / Self-Hosted Backend (OpenAI-Compatible)
1. Open **MacroBite ➔ Settings ➔ AI Settings**.
2. Enter your custom Base URL (e.g. `http://<tailscale-ip>:8000/v1` or `http://localhost:11434/v1`).
3. (Optional) Specify your model name (e.g. `Gemini 3.8 Flash (Low)`, `llama3`, `mistral`).
4. Enter your API Key or Bearer Token and tap **Test Connection** / **Save Key**. Cleartext HTTP is fully supported for local/Tailscale IPs.

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
