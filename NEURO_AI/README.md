# NEURO AI
Hybrid Android AI starter: Jetpack Compose client → FastAPI backend → configurable OpenAI-compatible model.

## Backend
Python 3.11+. From this directory:
```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
Set AI_BASE_URL, AI_API_KEY and AI_MODEL in backend/.env. Keep the provider key on the server only.

## Android
Open NEURO_AI/android in Android Studio. Set NEURO_API_BASE_URL in android/gradle.properties to the HTTPS backend URL ending with /.
The GitHub Actions workflow builds a debug APK.

## Scope
Included: Compose chat UI, FastAPI health/chat endpoints, provider adapter and CI build.
Next: accounts, PostgreSQL history, memory controls, image input and voice.
This is a starter, not a production-ready service.
