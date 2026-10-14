
import logging
import os
from typing import Literal

from fastapi import FastAPI, HTTPException
from openai import OpenAI
from pydantic import BaseModel, Field

from schemas import PredictionRequest, PredictionResponse
from predictor import predict_occupancy, classify_congestion

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Mirqab AI",
    description="Expo congestion prediction service",
    version="1.0.0"
)


@app.get("/health")
def health_check():
    return {"status": "healthy"}


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest):
    try:
        predicted_occupancy = predict_occupancy(
            snapshots=request.recentSnapshots,
            horizon_minutes=request.predictionHorizonMinutes
        )

        congestion_level = classify_congestion(
            predicted_occupancy=predicted_occupancy,
            capacity=request.capacity
        )

        return PredictionResponse(
            zoneId=request.zoneId,
            predictedOccupancy=predicted_occupancy,
            predictedCongestionLevel=congestion_level,
            horizonMinutes=request.predictionHorizonMinutes
        )

    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error))


class AnalysisRequest(BaseModel):
    simulationId: int = Field(gt=0)
    zoneId: int = Field(gt=0)
    capacity: int = Field(gt=0)
    predictedOccupancy: int = Field(ge=0)
    predictedCongestionLevel: Literal["LOW", "MEDIUM", "HIGH"]
    horizonMinutes: int = Field(default=10, ge=1)


@app.post("/analyze")
def analyze_simulation(request: AnalysisRequest):
    api_key = os.getenv("OPENAI_API_KEY", "").strip()

    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="AI analysis is not configured."
        )

    try:
        client = OpenAI(
            api_key=api_key,
            timeout=20.0,
            max_retries=1
        )

        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a crowd safety analyst. "
                        "Provide concise, practical recommendations "
                        "for event organizers. Treat predictions as "
                        "estimates, not facts. Do not invent input data. "
                        "Prioritize safety and explain when occupancy "
                        "approaches or exceeds capacity."
                    )
                },
                {
                    "role": "user",
                    "content": (
                        f"Simulation ID: {request.simulationId}\n"
                        f"Zone ID: {request.zoneId}\n"
                        f"Capacity: {request.capacity}\n"
                        f"Predicted occupancy: "
                        f"{request.predictedOccupancy}\n"
                        f"Predicted congestion: "
                        f"{request.predictedCongestionLevel}\n"
                        f"Prediction horizon: "
                        f"{request.horizonMinutes} minutes\n\n"
                        "Return a short analysis and actionable "
                        "recommendations based on these inputs."
                    )
                }
            ],
            temperature=0.3
        )

        analysis = response.choices[0].message.content

        if not analysis or not analysis.strip():
            logger.error("OpenAI returned an empty analysis.")
            raise HTTPException(
                status_code=502,
                detail="AI analysis returned an empty response."
            )

        return {
            "simulationId": request.simulationId,
            "zoneId": request.zoneId,
            "analysis": analysis.strip()
        }

    except HTTPException:
        raise

    except Exception:
        logger.exception("AI analysis request failed.")
        raise HTTPException(
            status_code=502,
            detail="AI analysis is temporarily unavailable. Please try again."
        )
