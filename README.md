# FitPick - AI gym routine app (Android, Kotlin, Jetpack Compose)

## Run it
1. Open the `FitPick` folder in Android Studio (Koala or newer) and let Gradle sync.
2. Run on an emulator or phone (Android 8.0+).
3. Open Settings in the app and paste a free Gemini API key (aistudio.google.com).

## Requirements checklist
| Requirement | Where |
|---|---|
| Multiple screens | Home, Create, Routine, Workout, Progress, Settings (ui/Screens.kt, MainActivity.kt) |
| Data persistence | Room (routines, workout logs: data/Db.kt), DataStore (API key, weekly goal: data/Settings.kt) |
| External API + coroutines | wger exercise API with Retrofit suspend functions, one parallel `async` per muscle (data/Repository.kt, data/Net.kt) |
| AI function | Gemini API builds the routine as JSON; also swaps a single exercise (data/Repository.kt) |

## Flow
wger API returns real exercise names and images for the chosen muscles -> Gemini picks and structures
the routine (sets, reps, rest, form tip) -> user saves, shares, or starts the workout -> logs go to Room
-> Progress screen shows the weekly chart.

If the network or key is missing, the app falls back to an offline routine so the demo never breaks.
