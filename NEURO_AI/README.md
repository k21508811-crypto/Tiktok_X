# NEURO AI

Android application that provides one place to open a directory of third-party AI assistants.

## Android app

The app currently includes:
- A redesigned dark interface with animated assistant selection.
- Search across the assistant directory.
- Embedded WebView pages for ChatGPT, Gemini, Copilot, DeepSeek, Perplexity, Qwen, Mistral Le Chat, HuggingChat, Claude, Grok, Yandex Alice, Poe and Duck.ai.
- A PRO information screen that does not falsely claim a payment has been processed.

Assistant websites control their own sign-in, regional availability, free tiers, usage limits, cookies and WebView compatibility. Listing a service does not mean its provider offers unlimited free access or a public API.

## Payments and PRO

RuStore payment integration is not complete. The app must not unlock PRO based on a local UI flag. Before enabling purchases, create the subscription/product in RuStore Console, provide its product ID, configure the current RuStore Pay SDK, and validate purchase status on the backend. The legacy BillingClient SDK is deprecated and its payment processing ended on 1 August 2026; use the current Pay SDK instead.

## Backend

The FastAPI backend exposes:
- `GET /health` — health check.
- `POST /api/v1/chat` — forwards messages to a configured OpenAI-compatible AI provider.

Configure the provider on the server using `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL` and `AI_TIMEOUT_SECONDS`. Never put provider API keys in the Android APK or a public repository.

## Build

Open `NEURO_AI/android` in Android Studio or run the **NEURO AI Android** GitHub Actions workflow. The expected debug artifact is `neuro-ai-debug-apk`.

## Remaining production work

1. Connect the native chat UI to the FastAPI endpoint.
2. Add accounts, server-side PRO entitlement checks and purchase validation.
3. Implement chat history, export and user-controlled deletion.
4. Test every provider's login and navigation on physical Android devices.
5. Add final store artwork and release signing.
