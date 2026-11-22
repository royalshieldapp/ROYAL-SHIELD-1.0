from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
import httpx
import logging
import time
from config.settings import settings

router = APIRouter(prefix="/api/assistant", tags=["Assistant"])
logger = logging.getLogger(__name__)

class ChatHistoryEntry(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    message: Optional[str] = None
    prompt: Optional[str] = None  # Compatible with legacy Android AiManager
    history: Optional[List[ChatHistoryEntry]] = Field(default_factory=list)
    model: Optional[str] = None

@router.post("/chat")
async def chat_proxy(payload: ChatRequest):
    # Support both "message" and "prompt" from Android clients
    user_text = (payload.message or payload.prompt or "").strip()
    if not user_text:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": "message or prompt is required", "code": "INVALID_REQUEST"}
        )

    provider = (settings.ai_provider or "nvidia").lower()

    if provider == "nvidia":
        return await _call_nvidia_qwen(user_text, payload.history, payload.model)
    elif provider == "gemini":
        return await _call_gemini_fallback(user_text, payload.history, payload.model)
    else:
        # Default fallback to NVIDIA Qwen
        return await _call_nvidia_qwen(user_text, payload.history, payload.model)


async def _call_nvidia_qwen(user_text: str, history: Optional[List[ChatHistoryEntry]], requested_model: Optional[str]):
    apiKey = settings.nvidia_api_key
    model_id = requested_model or settings.nvidia_model or "nvidia/nemotron-3.5-lightning-30b-a3b"
    timeout_sec = settings.ai_request_timeout_seconds or 30.0

    if not apiKey or apiKey == "your_nvidia_api_key_here":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "AI service not configured",
                "code": "AI_NOT_CONFIGURED",
                "message": "NVIDIA API key not configured on server. Set NVIDIA_API_KEY."
            }
        )

    # Build standard OpenAI-compatible messages for NVIDIA NIM
    messages = [
        {
            "role": "system",
            "content": (
                "You are Royal Security Sentinel (RS-Sentinel), the core AI agent of the Royal Shield cybersecurity ecosystem. "
                "Keep responses concise, technical, authoritative, and helpful."
            )
        }
    ]

    history_slice = history[-10:] if history else []
    for entry in history_slice:
        role = "assistant" if entry.role in ("assistant", "model") else "user"
        messages.append({"role": role, "content": entry.content})

    messages.append({"role": "user", "content": user_text})

    nvidia_url = "https://integrate.api.nvidia.com/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {apiKey}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": model_id,
        "messages": messages,
        "temperature": 0.6,
        "top_p": 0.9,
        "max_tokens": 1024
    }

    start_time = time.time()
    try:
        async with httpx.AsyncClient(timeout=timeout_sec) as client:
            response = await client.post(nvidia_url, json=payload, headers=headers)

        latency_ms = int((time.time() - start_time) * 1000)
        logger.info(f"AI_PROVIDER=nvidia AI_MODEL={model_id} AI_REQUEST_STATUS={response.status_code} AI_LATENCY_MS={latency_ms}")

        if response.status_code in (401, 403):
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail={"error": "NVIDIA authentication error", "code": "AI_AUTH_ERROR"}
            )
        elif response.status_code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail={"error": "NVIDIA rate limit exceeded", "code": "RATE_LIMITED", "retryAfter": 60}
            )
        elif response.status_code == 400:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"error": "Invalid request to AI service", "code": "INVALID_REQUEST"}
            )
        elif response.status_code >= 500:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail={"error": "NVIDIA inference server error", "code": "AI_UNAVAILABLE"}
            )
        elif response.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail={"error": f"AI service returned {response.status_code}", "code": "AI_UNAVAILABLE"}
            )

        data = response.json()
        choices = data.get("choices", [])
        if not choices:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail={"error": "AI returned empty choices", "code": "EMPTY_RESPONSE"}
            )

        reply = choices[0].get("message", {}).get("content", "").strip()
        if not reply:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail={"error": "AI returned empty content", "code": "EMPTY_RESPONSE"}
            )

        usage = data.get("usage", {})
        return {
            "success": True,
            "reply": reply,
            "response": reply,   # Compatibility with legacy AiManager
            "message": reply,    # Compatibility with legacy AiManager
            "provider": "nvidia",
            "model": model_id,
            "usage": {
                "promptTokens": usage.get("prompt_tokens"),
                "responseTokens": usage.get("completion_tokens")
            }
        }

    except httpx.TimeoutException:
        logger.error(f"[AI] NVIDIA timeout after {timeout_sec}s")
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail={"error": "AI service timeout", "code": "AI_TIMEOUT"}
        )
    except httpx.HTTPError as exc:
        logger.error(f"[AI] NVIDIA client connection error: {exc}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"error": "AI service communication error", "code": "AI_UNAVAILABLE"}
        )


async def _call_gemini_fallback(user_text: str, history: Optional[List[ChatHistoryEntry]], requested_model: Optional[str]):
    apiKey = settings.gemini_api_key
    if not apiKey or apiKey == "your_gemini_api_key_here":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={"error": "Fallback AI service not configured", "code": "AI_NOT_CONFIGURED"}
        )

    model_name = requested_model or "gemini-1.5-flash"
    gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={apiKey}"

    contents = []
    for entry in (history[-10:] if history else []):
        contents.append({
            "role": "model" if entry.role in ("assistant", "model") else "user",
            "parts": [{"text": entry.content}]
        })
    contents.append({"role": "user", "parts": [{"text": user_text}]})

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                gemini_url,
                json={"contents": contents},
                headers={"Content-Type": "application/json"}
            )

        if resp.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail={"error": "Fallback AI error", "code": "AI_UNAVAILABLE"}
            )

        data = resp.json()
        candidates = data.get("candidates", [])
        reply = candidates[0].get("content", {}).get("parts", [])[0].get("text", "") if candidates else ""

        return {
            "success": True,
            "reply": reply,
            "response": reply,
            "message": reply,
            "provider": "gemini",
            "model": model_name
        }
    except Exception as e:
        logger.error(f"[AI] Gemini fallback error: {e}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"error": "Fallback AI communication error", "code": "AI_UNAVAILABLE"}
        )


@router.get("/status")
def get_status():
    provider = (settings.ai_provider or "nvidia").lower()
    if provider == "nvidia":
        configured = bool(settings.nvidia_api_key and settings.nvidia_api_key != "your_nvidia_api_key_here")
        model = settings.nvidia_model or "qwen/qwen2.5-72b-instruct"
    else:
        configured = bool(settings.gemini_api_key and settings.gemini_api_key != "your_gemini_api_key_here")
        model = "gemini-1.5-flash"

    return {
        "service": "ai_assistant",
        "status": "available" if configured else "not_configured",
        "model": model,
        "provider": provider
    }
