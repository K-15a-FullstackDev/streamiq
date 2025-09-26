from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import Optional, Literal, Dict
import math

app = FastAPI(title="StreamIQ Classifier", version="0.1.0")

class Payload(BaseModel):
    # flexible payload: text events or sensor events
    text: Optional[str] = None
    user: Optional[str] = None
    device: Optional[str] = None
    temp: Optional[float] = None
    humidity: Optional[float] = None

class EventIn(BaseModel):
    event_id: str
    ts: str
    source: Literal["twitter","sensor"]
    payload: Payload

class ClassificationOut(BaseModel):
    label: str = Field(..., description="classification label")
    confidence: float = Field(..., ge=0, le=1)
    reasons: Dict[str, float] = {}

def classify_text(text: str) -> ClassificationOut:
    if not text:
        return ClassificationOut(label="unknown", confidence=0.5, reasons={})
    text_l = text.lower()
    # Very simple keyword scoring for demo; replace with ML later
    hints_pos = ["ok", "stable", "recovered", "resolved"]
    hints_neg = ["error", "fail", "degraded", "slow", "spike", "storm", "critical"]
    score = 0.0
    reasons = {}
    for w in hints_pos:
        if w in text_l:
            score += 0.6
            reasons[w] = reasons.get(w, 0) + 0.6
    for w in hints_neg:
        if w in text_l:
            score -= 0.7
            reasons[w] = reasons.get(w, 0) - 0.7

    # map score to [0,1] confidence via logistic
    conf = 1 / (1 + math.exp(-abs(score)))
    label = "incident" if score < 0 else ("normal" if score > 0.2 else "unknown")
    return ClassificationOut(label=label, confidence=float(round(conf, 3)), reasons=reasons)

def classify_sensor(temp: Optional[float], humidity: Optional[float]) -> ClassificationOut:
    if temp is None and humidity is None:
        return ClassificationOut(label="unknown", confidence=0.5, reasons={})
    reasons = {}
    risk = 0.0
    if temp is not None:
        if temp > 32: 
            risk += 1.0; reasons["temp_high"] = temp
        elif temp < 10:
            risk += 0.6; reasons["temp_low"] = temp
    if humidity is not None:
        if humidity > 75:
            risk += 0.8; reasons["humidity_high"] = humidity
        elif humidity < 20:
            risk += 0.4; reasons["humidity_low"] = humidity
    conf = 1 / (1 + math.exp(-risk))
    label = "anomaly" if risk >= 0.9 else ("warning" if risk >= 0.5 else "normal")
    return ClassificationOut(label=label, confidence=float(round(conf, 3)), reasons=reasons)

@app.get("/healthz")
def health():
    return {"ok": True}

@app.post("/classify")
def classify(ev: EventIn):
    if ev.source == "twitter":
        out = classify_text(ev.payload.text or "")
    else:
        out = classify_sensor(ev.payload.temp, ev.payload.humidity)
    return {
        "event_id": ev.event_id,
        "ts": ev.ts,
        "source": ev.source,
        "classification": out.model_dump(),
    }
