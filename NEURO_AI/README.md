# NEURO AI

Android application shell that opens the official ChatGPT website inside an Android WebView.

## Android build

Open `NEURO_AI/android` in Android Studio, or run the GitHub Actions workflow. The debug APK is uploaded as `neuro-ai-debug-apk`.

## How it works

- Launches `https://chatgpt.com/` inside the app.
- The user signs in with their own ChatGPT account.
- JavaScript, DOM storage and cookies are enabled for the web app session.
- Android Back navigates the web page history.
- If the embedded login or page is unsupported on a device, use **Open in browser**.

This is a WebView wrapper, not an official OpenAI Android client or an API integration. ChatGPT features, account limits, subscription entitlements and sign-in compatibility are controlled by OpenAI and may change. A ChatGPT subscription does not include API credits.

## Privacy and security

The app does not collect ChatGPT credentials or embed an API key. The ChatGPT website handles sign-in and its own session. Avoid adding code that reads page contents, passwords or session cookies.
