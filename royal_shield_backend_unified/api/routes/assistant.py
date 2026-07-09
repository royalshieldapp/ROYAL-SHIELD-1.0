from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
import httpx
import logging
from config.settings import settings

router = APIRouter(prefix="/api/assistant", tags=["Assistant"])
logger = logging.getLogger(__name__)

class ChatHistoryEntry(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)
    history: Optional[List[ChatHistoryEntry]] = Field(default_factory=list)
    model: str = "gemini-1.5-flash"

@router.post("/chat")
async def chat_proxy(payload: ChatRequest):
    apiKey = settings.gemini_api_key

    if not apiKey or apiKey == "your_gemini_api_key_here":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "error": "AI service not configured",
                "code": "AI_NOT_CONFIGURED",
                "message": "Gemini API key not set on the server. Configure GEMINI_API_KEY in environment variables."
            }
        )

    # Build conversation contents for Gemini
    contents = []

    # Add history if provided (keep last 10 messages)
    history_slice = payload.history[-10:] if payload.history else []
    for entry in history_slice:
        contents.append({
            "role": "model" if entry.role == "assistant" else "user",
            "parts": [{"text": entry.content}]
        })

    # Add current message
    contents.append({
        "role": "user",
        "parts": [{"text": payload.message}]
    })

    gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/{payload.model}:generateContent?key={apiKey}"

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                gemini_url,
                json={
                    "contents": contents,
                    "generationConfig": {
                        "temperature": 0.7,
                        "topK": 40,
                        "topP": 0.95,
                        "maxOutputTokens": 2048
                    },
                    "safetySettings": [
                        { "category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE" },
                        { "category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_MEDIUM_AND_ABOVE" },
                        { "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_MEDIUM_AND_ABOVE" },
                        { "category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE" }
                    ]
                },
                headers={"Content-Type": "application/json"}
            )

        if response.status_code == 429:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail={"error": "AI rate limit exceeded", "code": "RATE_LIMITED", "retryAfter": 60}
            )
        elif response.status_code == 400:
            res_data = response.json()
            err_msg = res_data.get("error", {}).get("message", "Invalid request to AI service")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"error": "Invalid request to AI service", "code": "INVALID_REQUEST", "details": err_msg}
            )
        elif response.status_code != 200:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail={"error": "AI service temporarily unavailable", "code": "AI_UNAVAILABLE"}
            )

        data = response.json()
        candidates = data.get("candidates", [])
        candidate = candidates[0] if candidates else {}
        parts = candidate.get("content", {}).get("parts", [])
        reply = parts[0].get("text") if parts else None

        if not reply:
            finish_reason = candidate.get("finishReason")
            if finish_reason == "SAFETY":
                raise HTTPException(
                    status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                    detail={"error": "Response blocked by safety filters", "code": "SAFETY_BLOCKED"}
                )
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail={"error": "AI returned empty response", "code": "EMPTY_RESPONSE"}
            )

        usage_metadata = data.get("usageMetadata", {})
        return {
            "success": True,
            "reply": reply,
            "model": payload.model,
            "usage": {
                "promptTokens": usage_metadata.get("promptTokenCount"),
                "responseTokens": usage_metadata.get("candidatesTokenCount")
            }
        }

    except httpx.HTTPError as exc:
        logger.error(f"[AI] Gemini client error: {exc}")
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail={"error": "AI service communication error", "code": "AI_UNAVAILABLE"}
        )

@router.get("/status")
def get_status():
    apiKey = settings.gemini_api_key
    configured = apiKey is not None and apiKey != "your_gemini_api_key_here"

    return {
        "service": "ai_assistant",
        "status": "available" if configured else "not_configured",
        "model": "gemini-1.5-flash",
        "provider": "google"
    }
