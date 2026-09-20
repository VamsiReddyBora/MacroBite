# 🍽️ MacroBite — Minimalist Android Macro & Nutrition Tracker

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg?style=flat&logo=android)](https://www.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat)](LICENSE)

A lightweight, distraction-free native Android app for rapid food logging, daily macro tracking, and high-calorie coaching. Designed for students, athletes, and anyone tired of slow, ad-cluttered calorie tracking apps.

**No subscriptions. No ads. No background tracking. 100% on-device SQLite database.**

---

## 📖 Why MacroBite?

Most food tracking apps on the Play Store today suffer from the same problems:
1. **Paywalls & Subscriptions:** Basic features like scanning a barcode or seeing a macro breakdown are locked behind monthly paywalls.
2. **Clutter & Ads:** Intrusive popups, social media feeds, and bloated loading screens that take 20 seconds just to log a glass of milk.
3. **Western-Centric Food Databases:** They struggle to recognize home-cooked meals, regional dishes (like rotis, paneer bhurji, dal, sattu, or mess thalis), or local packaged snacks.

MacroBite was built to fix this. It runs entirely on your device with local Room persistence, offers a fast offline dictionary parser, connects optionally to Google Gemini for multimodal AI vision & conversational queries, and includes a zero-flash home screen widget so you can log food in 2 seconds flat without opening the app.

---

## ✨ Key Features at a Glance

* **⚡ Ultra-Fast Logging:** Type naturally (*"2 eggs with 2 slices of butter toast and chai"*), speak using voice recognition, snap a photo of your meal, or scan a product barcode.
* **🧠 Dual-Engine Parsing:**
  * **Offline Heuristic Parser:** Zero internet required. Matches Indian and global food staples instantaneously.
  * **Gemini AI Vision & Text:** Free Google Gemini API integration (`gemini-2.5-flash` / `gemini-3.5-flash-lite`) for detailed nutritional breakdowns and photo recognition.
* **🏷️ Barcode Scanner with Offline Memory:** Scans OpenFoodFacts barcodes. If a local snack or unknown product isn't found in online databases, simply photograph its back-of-pack nutrition table once — MacroBite memorizes it locally and recognizes that barcode forever offline.
* **📱 Home Screen Action Widget:** A sleek search bar widget with glowing dynamic borders. Tap the text box to type or tap the mic to speak; it parses and logs your meal silently in the background with zero screen flashing.
* **🤖 Voice Assistant & Hands-Free Device Actions:** Beyond nutrition, the assistant can make phone calls, draft WhatsApp/SMS messages, start kitchen timers, set alarms, check battery status, and open maps or YouTube workouts.
* **👥 Contact Aliases:** Assign contact nicknames (e.g., `"dad" ➔ "Daddy"`) so voice commands dial your contacts directly without asking for disambiguation.
* **🔔 Smart Reminders & In-App Notification HUD:** AI Well-Wisher meal notifications scheduled with exact Android alarms, plus modern floating in-app HUD capsules with full toggle controls in Settings.
* **👟 Google Health Connect Integration:** Automatically synchronizes footsteps, walking distance, and active calorie burn from Google Fit / Health Connect.
* **📈 Body Weight & Macro Analytics:** Interactive weekly calorie charts, BMI calculator, weight trend analysis, and 1-tap CSV export for Google Sheets/Excel.
* **🎨 6 Handcrafted Themes:** Amber Gold, Emerald Green, Cobalt Blue, Crimson Red, Amethyst Purple, and Slate Gray in both Dark and Light modes.

---

## 📱 User Manual: How to Use MacroBite

### 1. The Dashboard (Main Log Screen)
* **Macro Gauge:** The circular calorie gauge at the top shows your consumed calories against your daily target (e.g., `2,150 / 3,000 kcal`). Below it, three horizontal bars track your **Protein**, **Carbs**, and **Fats** in grams.
* **Hostel / High-Calorie Tip Card:** If you are trying to gain weight or hit high targets, MacroBite calculates how many calories you have left in the evening and offers a quick 1-tap top-up snack (e.g., *"Hostel Quick Top-up: 1 glass milk + 2 tbsp peanut butter + banana (+650 kcal)"*).
* **Quick Log Bar:** Located at the bottom of the screen:
  * **Text Input:** Type what you ate in plain English (e.g., *"3 rotis with paneer curry and 1 bowl rice"*). Press **Send**.
  * **Camera Icon:** Tap to snap a picture of your plate or packaged food. The AI analyzes portion sizes and breaks down macros.
  * **⚡ Quick Presets (Bolt Icon):** Opens a bottom sheet with hostel & athlete staples (Whole Milk, Boiled Eggs, Bananas, Peanut Butter, Hostel Thali, Oats Shake, Maggi, Chicken Curry) for instant 1-tap logging.
  * **Barcode Icon:** Opens the live camera barcode scanner.
* **Meal Feed:** Meals are grouped by **Breakfast**, **Lunch**, **Snacks**, and **Dinner**. You can tap any meal to inspect portion details, edit macros, or swipe left to delete (with a 5-second floating undo bar).

---

### 2. Barcode Scanning & The Offline Food Memory
When shopping or snacking:
1. Tap the **Barcode Scanner** icon in the Quick Log bar.
2. Align the barcode inside the camera viewfinder.
3. If the item is in the OpenFoodFacts database, its calories, protein, carbs, and fats pop up instantly. Tap **Log Meal**.
4. **What if the barcode is not in the database?**
   * If you scan an unknown local snack or specialty food, MacroBite will let you know: *"Barcode Not Found"*.
   * Tap the **"Save to Local Database"** button right inside the scanner dialog.
   * Point your camera at the **Nutrition Information Table** on the back of the package and snap a photo.
   * MacroBite's AI extracts the exact calories and macros.
   * **The Magic:** MacroBite pairs that barcode with the extracted nutrition facts and permanently saves it in your local database. The next time you scan that exact barcode — even months later and completely offline — the details appear immediately!

---

### 3. Setting Up and Using the Home Screen Widget
MacroBite includes an interactive home screen search widget:
1. Long-press any empty space on your Android home screen and tap **Widgets**.
2. Scroll to **MacroBite** and drag the **MacroBite Assistant** widget onto your home screen.
3. Resize the widget horizontally to match your grid (4x1 or 5x1 recommended).
4. **How to use it:**
   * **Quick Text Logging:** Tap the text area. An unobtrusive floating input box appears with the keyboard open. Type what you ate (e.g., *"2 boiled eggs"*), hit enter, and it logs silently in the background — no full app launch, no screen flashing.
   * **Quick Voice Logging:** Tap the **Microphone** icon. Speak your meal or command (e.g., *"1 glass milk and 2 bananas"*). The audio is transcribed and logged directly.
   * **Quick Camera Scan:** Tap the **Camera** icon to immediately launch camera capture.

---

### 4. Voice Assistant & Device Controls
MacroBite includes an on-device device action engine accessible from the Chat screen and the widget. In addition to asking nutrition questions, you can give it real-world device commands:
* 📞 **Phone Calls:** *"Call Mom"* or *"Phone 9876543210"*.
* 💬 **WhatsApp Messages:** *"Send WhatsApp to Rahul: Reached the gym"*.
* 💬 **SMS Messages:** *"Text Sam: Pick up eggs on your way home"*.
* ⏰ **Timers:** *"Set timer for 12 minutes for boiled eggs"*.
* ⏰ **Alarms:** *"Set alarm for 6:30 AM"*.
* 🗺️ **Navigation & Maps:** *"Directions to nearest gym"* or *"Navigate to Subway"*.
* 🎬 **YouTube:** *"Open YouTube"* or *"Search chest workout on YouTube"*.
* 🔦 **Flashlight:** *"Turn on flashlight"* or *"Turn off torch"*.
* 🔋 **Battery:** *"Check battery status"*.

#### Setting Up Contact Aliases:
To make calling instant without phonebook permission prompts:
1. Open **Settings** ➔ **Contact Aliases**.
2. Enter a nickname and your phone contact's exact name (e.g., Alias: `mom` ➔ Target: `Mother` or Alias: `dad` ➔ Target: `Daddy`).
3. Now whenever you say *"Call dad"*, MacroBite immediately dials the right contact.

---

### 5. Managing In-App & Status Bar Notifications
MacroBite gives you full control over how and when it notifies you:
* Open **Settings** ➔ **Notifications & Meal Reminders**:
  * **Master Toggle:** Turn all automated reminders on or off.
  * **AI Well-Wisher Reminders:** Set custom reminder times for Breakfast (e.g. `08:30`), Lunch (`13:00`), Snack (`17:30`), Dinner (`20:30`), and Daily Summary (`21:30`).
  * **In-App Floating Notifications:** Toggle the animated spring-action capsules on/off. When turned off, snackbar popups are completely suppressed so you can browse the app with zero UI interruptions.
  * **Food Logged Status Bar Alerts:** Optional system status bar notifications when a meal is logged.
  * **Token Budget Alerts:** Optional warnings if your Gemini API usage crosses 50%, 75%, or 90% of your daily target.

---

### 6. Health Connect & Step Tracking
1. Install **Health Connect** (built into Android 14+, or downloadable from the Play Store on Android 9–13).
2. Link your preferred fitness app (Google Fit, Samsung Health, etc.) to Health Connect.
3. In MacroBite, go to **Settings** ➔ **Google Health & Activity**.
4. Enable sync and grant permission for **Steps** and **Calories Burned**.
5. Your daily steps and active calorie expenditure will now display directly on your MacroBite dashboard.

---

### 7. Weight Tracker, BMI & CSV Backups
* **Weight Tracker:** In **Settings** or the bottom of the Dashboard, enter your current weight in kg. MacroBite graphs your trend and shows net gain/loss against your starting weight.
* **BMI Calculator:** Enter your height in cm and weight in kg to see your current Body Mass Index and healthy weight category.
* **CSV Export:** Go to **Settings** ➔ **Local & Cloud Backup** ➔ **Export Meal History to CSV**. MacroBite generates a clean, standardized `.csv` file and opens Android's system share sheet so you can send it to Google Drive, WhatsApp, or email.

---

## 🛠 Setup & Building from Source

### Prerequisites
* **Android Studio:** Hedgehog (2023.1.1) or newer (Iguana, Jellyfish, or Koala recommended)
* **Java Development Kit (JDK):** Version 17
* **Android SDK:** Compile SDK 34, Min SDK 26

### Step 1: Clone the Repository
```bash
git clone https://github.com/VamsiReddyBora/MacroBite.git
cd MacroBite
```

### Step 2: Configure Local SDK (CLI only)
If building from the command line without Android Studio:
```bash
cp local.properties.example local.properties
```
Open `local.properties` and verify `sdk.dir` points to your Android SDK directory (e.g., `sdk.dir=/Users/<your-username>/Library/Android/sdk` or `/home/<your-username>/Android/Sdk`).

### Step 3: Build the APK
Use the included Gradle wrapper:

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Run Unit Tests
./gradlew testReleaseUnitTest
```
*(On Windows, replace `./gradlew` with `gradlew.bat`)*

The compiled APK will be located at:
`app/build/outputs/apk/release/MacroBite.apk` (or `app-release.apk`)

---

## 🔑 Adding Your Free Gemini API Key (Optional)

MacroBite works out-of-the-box offline using its built-in heuristic food dictionary. If you want natural language reasoning, AI vision scanning, and conversational advice:

1. Visit [Google AI Studio](https://aistudio.google.com/) and sign in with your Google account.
2. Click **"Get API key"** and create a new key (free tier includes generous daily requests).
3. Open the **MacroBite** app on your phone.
4. Go to **Settings** ➔ **Gemini AI Settings**.
5. Paste your API key and tap **Save Key**.
6. Tap **Test Connection** to verify that your key is active and responding.

> **Security Note:** Your API key is stored strictly on your local device using Android Jetpack DataStore. It is never transmitted to any third-party developer server; network requests go directly from your phone to Google's official Gemini endpoint.

---

## 🏗 Architecture & Codebase Overview

MacroBite follows **Clean Architecture** principles combined with MVVM and Unidirectional Data Flow (UDF):

```
MacroBite/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/macrobite/app/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/          # Room Database (Entities: Meal, Weight, Chat, CustomFood)
│   │   │   │   │   ├── parser/         # Offline LocalFoodDatabase & GeminiFoodParser
│   │   │   │   │   ├── preferences/    # Jetpack DataStore (UserPreferencesRepositoryImpl)
│   │   │   │   │   ├── scanner/        # BarcodeNutritionResolver (OpenFoodFacts API)
│   │   │   │   │   └── repository/     # Repository implementations
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/          # Pure Kotlin data classes (MealEntry, UserTargets, etc.)
│   │   │   │   │   ├── repository/     # Repository interfaces
│   │   │   │   │   └── usecase/        # Use cases (ParseFoodUseCase, ExportCsvUseCase, etc.)
│   │   │   │   ├── health/             # Health Connect integration manager
│   │   │   │   ├── notification/       # Exact Alarm scheduler, BootReceiver, AiWellWisherCoach
│   │   │   │   ├── widget/             # MacroBiteChatWidgetProvider & background activities
│   │   │   │   ├── ui/
│   │   │   │   │   ├── dashboard/      # DashboardScreen, MacroGaugeWidget, QuickLogBar
│   │   │   │   │   ├── chat/           # ChatScreen, Voice recognizer, Device action engine
│   │   │   │   │   ├── history/        # HistoryScreen, WeeklyBarChart
│   │   │   │   │   ├── scanner/        # BarcodeNutritionScannerDialog (ML Kit + CameraX)
│   │   │   │   │   ├── settings/       # SettingsScreen, ThemePicker, ContactAliases
│   │   │   │   │   ├── common/         # Floating Notification HUD, Undo bar, Modifiers
│   │   │   │   │   └── theme/          # Material 3 Color Schemes & 6 Theme Presets
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                    # Vector assets, widget layouts, sound effects
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Unit test suite (Parsers, Reminders, Actions)
│   └── build.gradle
├── gradlew / gradlew.bat               # Gradle wrapper executable
└── build.gradle / settings.gradle
```

---

## 🔒 Permissions & Privacy Policy

MacroBite is built on a strict privacy-first foundation:
* **Camera (`android.permission.CAMERA`):** Used only when you choose to take a photo of food or scan a barcode. Images are never uploaded to any server other than your direct calls to Google Gemini if AI mode is enabled.
* **Microphone (`android.permission.RECORD_AUDIO`):** Used only when you tap the microphone button for speech-to-text. Audio is transcribed on-device via Android SpeechRecognizer.
* **Contacts (`android.permission.READ_CONTACTS`):** Used strictly on-device to match names and contact aliases when you issue voice call commands.
* **Phone (`android.permission.CALL_PHONE`):** Used to launch direct phone calls when requested by you.
* **Alarms (`android.permission.SCHEDULE_EXACT_ALARM`):** Used to trigger precise meal reminders even in Android Doze mode.
* **Health Connect:** Read-only access to steps and calories burned; requires explicit runtime authorization.
* **No Analytics / No Ads:** The app contains zero advertising SDKs, tracking pixels, or third-party telemetry.

---

## 🤝 Contributing

Contributions are welcome! Whether it is adding new regional foods to `LocalFoodDatabase.kt`, improving widget designs, or fixing bugs:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to your branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

---

## 📄 License

MacroBite is open-source software licensed under the [MIT License](LICENSE).
Feel free to use, modify, and distribute it for personal and commercial projects.
