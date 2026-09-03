# Jarvis — Personal Phone Assistant (Android)

A minimal but real voice/text assistant for Android. It can:
- Chat with you (voice or text) using Claude's API
- **"Call [contact name]"** → looks up the contact, opens the dialer with their number
- **"Open [app name]"** → launches that app on your phone

## 1. Requirements
- [Android Studio](https://developer.android.com/studio) (free)
- A physical Android phone (or emulator) running Android 8.0+
- An Anthropic API key from https://console.anthropic.com/ (for the chat "brain")

## 2. Setup
1. Open Android Studio → **Open** → select this `JarvisApp` folder.
2. Let Gradle sync (first time takes a few minutes, downloads dependencies).
3. Open `app/src/main/java/com/example/jarvis/AssistantEngine.kt` and replace:
   ```
   private const val API_KEY = "YOUR_ANTHROPIC_API_KEY_HERE"
   ```
   with your real key.
4. Connect your phone via USB (enable Developer Options → USB debugging) or start an emulator.
5. Click **Run ▶** in Android Studio.

## 3. First run
- The app will ask for microphone, contacts, and phone permissions — allow them.
- Tap 🎤 and say something like *"call Mom"*, *"open Spotify"*, or *"what's the weather like in general?"*
- Or type a command and hit Send.

## 4. How it decides what to do
`AssistantEngine.kt` checks the command:
- Starts with `"call "` → contact lookup → opens dialer (`ACTION_DIAL`, no accidental auto-dial)
- Starts with `"open "` → matches installed app labels → launches it
- Anything else → sent to Claude's API, response is shown and spoken aloud

## 5. Important notes
- **Your API key lives in the app code.** Fine for a personal build-it-yourself app on your own device; do NOT publish this app publicly with the key embedded — anyone could extract it and rack up charges on your account. For a public app, route requests through your own backend server instead.
- `ACTION_DIAL` opens the dialer pre-filled but requires you to tap call — safer default. If you want fully automatic calling, switch to `ACTION_CALL` in `AssistantEngine.kt` (requires the `CALL_PHONE` permission, already declared).
- Want more actions (send a text, set an alarm, open a specific website)? Add more `if (lower.startsWith(...))` branches in `AssistantEngine.kt` — the pattern is easy to extend.

## 6. Building an APK from your phone (no computer needed)
This project includes a GitHub Actions workflow (`.github/workflows/build.yml`) that compiles an installable APK in the cloud. Your API key is **never stored in the code** — it's injected at build time from a GitHub secret. Everything below can be done from your phone's browser.

1. **Create a free GitHub account** at github.com if you don't have one.
2. **Create a new repository** (e.g. "jarvis-app"), then upload this entire `JarvisApp` folder's contents to it (GitHub's web UI lets you drag-and-drop files, or use "Add file → Upload files"). The code has no key in it — safe to upload as-is, public or private repo.
3. Add your key as a secret: go to your repo → **Settings → Secrets and variables → Actions → New repository secret**.
   - Name: `ANTHROPIC_API_KEY`
   - Value: your actual key from https://console.anthropic.com/
   - Save.
4. Go to the **Actions** tab. Click **"Build Jarvis APK"** → **Run workflow** → **Run workflow** (green button). GitHub automatically pulls the secret into the build — you don't touch it again.
5. Wait ~3-5 minutes for the green checkmark.
6. Click into the completed run → scroll to **Artifacts** → download **jarvis-debug-apk** (a `.zip` containing the `.apk`).
7. On your phone, open the zip, extract `app-debug.apk`, tap it to install. Allow **"Install unknown apps"** for your browser/file manager when prompted (one-time setting).
8. Open Jarvis and grant the mic/contacts/phone permissions.

**Building locally instead?** Set the same variable in your terminal before running Gradle: `export ANTHROPIC_API_KEY=your-key-here` (macOS/Linux) or `set ANTHROPIC_API_KEY=your-key-here` (Windows), then build/run from Android Studio as normal.

## 7. Extending it further
Ideas if you want to go beyond this prototype:
- Wake-word detection ("Hey Jarvis") using [Picovoice Porcupine](https://picovoice.ai/)
- Background service so it listens even when the app isn't open (needs a foreground service + notification)
- Smart-home control via Google Home API or local integrations
- Persistent conversation memory (store chat history in a local database)
