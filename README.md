# TicTacToe (Android)

>A modern, themeable Tic‑Tac‑Toe Android app with persistent high scores and an AI opponent.

**Features**

- **Themeable UI:** multiple color themes (Default, Blue, Green, Red, Gold, Silver) and system-aware title colors.
- **AI opponent:** Easy (random), Medium (win/block heuristic), Hard (minimax).
- **Persistent high scores:** stored in `SharedPreferences` (top 5 tracked per player).
- **Polished UI:** gradient board backgrounds, vector X/O icons that respect theme contrast, Material components.
- **Gameplay safeguards:** user input disabled while AI is "thinking" (1.2s delay) to avoid races.

**Getting Started**

Requirements:

- Java JDK (11+ recommended)
- Android SDK (Android Studio recommended)
- Gradle (wrapper included)

Build and install (from project root):

```bash
./gradlew assembleDebug
# then install the APK to a connected device/emulator (example):
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the project in Android Studio for easier testing and theming previews.

**Project Layout (key files)**

- `app/src/main/java/com/sunwings/tic_tac_toe/GameActivity.kt` — gameplay and AI logic.
- `app/src/main/java/com/sunwings/tic_tac_toe/SettingsActivity.kt` — theme selection and preferences.
- `app/src/main/java/com/sunwings/tic_tac_toe/HighScoreActivity.kt` — high score display and formatting.
- `app/src/main/res/drawable/ic_ttt_x.xml`, `ic_ttt_o.xml` — vector icons that use theme text color.
- Theme and color resources: `app/src/main/res/values/themes.xml`, `values-night/themes.xml`, `values/colors_themes.xml`.

**Theming & Contrast**

Light-colored themes (Gold, Silver) use dedicated contrast tokens so UI text and icons switch to dark (black) for readability. The main title and other system-following elements use a system-aware color resource that follows the phone's light/dark mode.

**Contributing**

Contributions are welcome. Please open issues or pull requests with proposed improvements or bug fixes. Keep changes focused and include a short description of what you changed and why.

**License**

This repository does not include a license file by default. Add a `LICENSE` if you intend to release under a specific license (MIT, Apache 2.0, etc.).

**Contact**

For questions or help running the app, open an issue or contact the maintainer in the repo.