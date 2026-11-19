import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from .config import settings

app = FastAPI(title="NEURO AI API", version="0.1.0")

class Message(BaseModel):
    role: str = Field(pattern="^(user|assistant)$")
    content: str = Field(min_length=1, max_length=12000)

class ChatRequest(BaseModel):
    messages: list[Message] = Field(min_length=1, max_length=40)

@app.get("/health")
async def health():
    return {"status": "ok", "service": "neuro-ai-api"}

@app.post("/api/v1/chat")
async def chat(request: ChatRequest):
    if not settings.ai_api_key or not settings.ai_model:
        raise HTTPException(503, "AI provider is not configured on the server.")
    payload = {
        "model": settings.ai_model,
        "messages": [
            {"role": "system", "content": "You are NEURO AI. Reply clearly and helpfully in the user's language."},
            *[m.model_dump() for m in request.messages],
        ],
        "temperature": 0.7,
    }
    try:
        async with httpx.AsyncClient(timeout=settings.ai_timeout_seconds) as client:
            response = await client.post(
                settings.ai_base_url.rstrip("/") + "/chat/completions",
                json=payload,
                headers={"Authorization": f"Bearer {settings.ai_api_key}"},
            )
            response.raise_for_status()
            answer = response.json()["choices"][0]["message"]["content"]
            if not isinstance(answer, str) or not answer.strip():
                raise ValueError("Empty model response")
            return {"reply": answer.strip(), "model": settings.ai_model}
    except httpx.HTTPStatusError as exc:
        raise HTTPException(502, "AI provider returned an error.") from exc
    except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as exc:
        raise HTTPException(502, "Could not get a valid response from the AI provider.") from exc
