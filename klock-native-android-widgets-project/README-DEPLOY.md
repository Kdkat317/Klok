# Klock — Live Deployment & Cloud APK Build Guide

## Option 1: Make Live on the Web in 15 Seconds (From Your Android Phone)
1. Open **https://app.netlify.com/drop** or **https://pages.github.com** in Chrome on your phone.
2. Upload the `web-pwa` folder (or just `index.html`, `manifest.json`, and `sw.js`).
3. Visit the generated `https://...` URL on your phone and tap **Chrome Menu (⋮) -> Add to Home screen / Install App**.

## Option 2: Compile the Real Android APK + Home-Screen Widgets in the Cloud (No PC Needed)
1. Create a free repository on **https://github.com/new** from your mobile browser.
2. Upload the contents of this ZIP (`android-project/` and `.github/workflows/build-apk.yml`).
3. Tap the **Actions** tab in your GitHub repository -> select **Build Klock Android APK & Widgets** -> tap **Run workflow**.
4. In ~2 minutes, download `Klock-Vedic-Android-Debug-APK.zip` from the workflow run summary and install `app-debug.apk` on your phone!
