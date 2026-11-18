# AI Neuro

Android application for the AI Neuro platform.

## Android app

The native app includes:
- **Home** — AI Neuro beta landing screen.
- **CLOUD** — embedded WebView for the official Cloud.com website.
- **Алиса AI** — embedded WebView for Yandex Alice AI at https://alice.yandex.ru.
- **PREMIUM CHATGPT** — locked entry point reserved for AI Neuro PRO subscribers.

The embedded Cloud and Alice pages are third-party services. Their sign-in, availability, cookies, and features are controlled by their providers and may vary by device or region.

## Premium and billing status

The ChatGPT section is currently locked in the app UI. The PRO dialog explicitly reports that billing and server-side entitlement checks are not connected yet. No payment is collected and no subscription is fabricated.

A production release must implement RuStore billing and validate purchase tokens on the backend before granting access. A client-side lock alone is not a secure subscription check.

## Backend

The FastAPI backend exposes:

- `GET /health` — service health check.
- `POST /api/v1/chat` — sends conversation messages to the configured OpenAI-compatible provider.

Configure the provider on the server with environment variables:

- `AI_BASE_URL`
- `AI_API_KEY`
- `AI_MODEL`
- `AI_TIMEOUT_SECONDS`

Never place provider API keys in the Android APK or commit them to the repository.

## Build

Open `NEURO_AI/android` in Android Studio, or run the **NEURO AI Android** GitHub Actions workflow. The debug APK artifact is named `neuro-ai-debug-apk`.

## Remaining work

1. Add user accounts and server-side PRO entitlement checks.
2. Connect RuStore billing and validate purchases on the backend.
3. Connect the native AI Neuro chat UI to `POST /api/v1/chat`.
4. Add chat history and user-controlled data deletion.
5. Add the final user-provided launcher icon and splash artwork.
