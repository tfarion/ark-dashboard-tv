# ARK Dashboard (Android TV)

A full-screen **kiosk WebView** for the workshop TV (Sony Google TV). It loads the public
ARK dashboard — `https://ark.knektd.com/dashboard` — which needs **no login** (the server
gates it by the TV's IP). Declared as a **HOME launcher**, so once set as default the TV
boots straight into the dashboard, hiding the Google TV home and its bloat. No fork of any
third-party launcher, nothing to re-patch on their updates — this app is entirely ours.

## What it does
- Full-screen, immersive, screen kept on.
- Auto-retries on network loss (TV up before Wi-Fi, router reboot) and reloads once a day.
- Swallows BACK; `MENU` on the remote forces a refresh.
- ARK-dark splash + placeholder ARK mark (replace with the real logo — see below).

## Build the APK
No Android SDK is bundled here — build it on a machine with Android Studio (easiest) or the
Android command-line SDK.

**Android Studio:** open this folder → it bootstraps the Gradle wrapper and SDK → Build ▸
*Build Bundle(s) / APK(s)* ▸ *Build APK(s)*. Output:
`app/build/outputs/apk/release/app-release.apk` (debug-signed, installable as-is).

**Command line** (SDK + a Gradle 8.7 wrapper present):
```bash
./gradlew assembleRelease
```
If the wrapper jar isn't here yet, generate it once with a local Gradle: `gradle wrapper`.

## Install on the TV
1. Enable **Developer options** + **Wireless debugging** on the TV (Settings ▸ System ▸ About ▸
   Build ×7, then System ▸ Developer options).
2. From your computer:
   ```bash
   adb connect <TV-IP>:5555      # or: adb pair <IP>:<port> first, on Android 11+
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
3. Make sure the **TV's IP is on the dashboard allowlist** (the server only serves `/dashboard`
   to allowed IPs).

## Set it as the default launcher (boot-to-dashboard)
Press **HOME** on the remote → pick **ARK Dashboard** → *Always*. Or via adb:
```bash
adb shell cmd package set-home-activity com.knektd.arktv/.MainActivity
```
**To get back to Google TV later** (to change settings or uninstall), restore the stock home:
```bash
adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.MainActivity
# then, to remove the kiosk:
adb uninstall com.knektd.arktv
```
(The stock launcher package name varies by model — `adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` lists the options.)

## Customise
- **Dashboard URL / reload interval:** constants at the bottom of
  `app/src/main/java/com/knektd/arktv/MainActivity.kt`.
- **Logo / branding:** replace `app/src/main/res/drawable/ark_mark.xml` (splash + icon) and
  `ic_banner.xml` (the TV app-row banner) with the real ARK artwork, or drop a 512px PNG as
  `res/mipmap-*/ic_launcher.png` and point `android:icon` at it in the manifest.
