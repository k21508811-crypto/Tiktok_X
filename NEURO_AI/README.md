# AI Neuro

Android application for the AI Neuro platform.

## Android app

The current beta build presents the native AI Neuro Chat access screen. Chat access is shown as a PRO feature and remains locked until subscription verification and billing are implemented.

The **ОФОРМИТЬ PRO** button currently explains that billing is not yet connected. It does not collect payment details or pretend that a subscription has been purchased.

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

Open `NEURO_AI/android` in Android Studio, or run the GitHub Actions workflow. The debug APK is uploaded as `neuro-ai-debug-apk`.

## Next implementation steps

1. Add user accounts and server-side PRO entitlement checks.
2. Connect RuStore billing and validate purchases on the backend.
3. Unlock the native chat only after the backend confirms an active subscription.
4. Connect the Android chat UI to `POST /api/v1/chat`.
5. Add chat history and user-controlled data deletion.

The current Android beta screen is a UI gate; it is not yet proof of a completed billing integration or an end-to-end chat release.
