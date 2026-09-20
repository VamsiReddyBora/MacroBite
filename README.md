# MacroBite 🍽️

A minimalist, privacy-first Android macro and calorie tracker. Built with 100% Jetpack Compose and Room.

**Zero ads. Zero subscriptions. Works 100% offline.**

---

## ⚡ Highlights

- **Fast Logging:** Type what you ate in plain text (*"2 eggs and butter toast"*), speak via mic, snap a photo, or scan barcodes.
- **Offline Barcode Memory:** Scans OpenFoodFacts. If an unknown barcode isn't found, snap its nutrition label once — MacroBite memorizes it locally forever offline.
- **Home Screen Widget:** Log meals or speak queries directly from your home screen in the background without opening the app.
- **AI Copilot & Device Actions:** Ask nutrition questions, or hands-free control your device: *"Call Mom"*, *"Set timer for 10 mins"*, *"WhatsApp Rahul"*, *"Open YouTube"*.
- **Contact Aliases:** Set voice nicknames (e.g. `"dad" ➔ "Daddy"`) for instant phone dialing.
- **Health Connect:** Sync daily steps and active calorie expenditure.
- **Privacy First:** All meal logs, body weight history, and data stay on your device in a local SQLite database.

---

## 🚀 Quick Start

### Build from Source
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

MacroBite works 100% offline with its built-in local food dictionary.

To enable AI photo scanning and conversational advice:
1. Get a free API key at [Google AI Studio](https://aistudio.google.com/).
2. Open **MacroBite ➔ Settings ➔ Gemini AI Settings**.
3. Paste your key and tap **Save Key**.

---

## 🛠 Tech Stack

- **Kotlin** & **Jetpack Compose** (Material 3)
- **Room Database** & **Jetpack DataStore**
- **Dagger Hilt** & **Coroutines / Flow**
- **Google Generative AI SDK** & **ML Kit Barcode Scanning**
- **Android Health Connect**

---

## 📄 License

Licensed under the [MIT License](LICENSE).
