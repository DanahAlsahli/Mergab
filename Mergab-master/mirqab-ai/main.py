from fastapi import FastAPI, HTTPException
import os
from openai import OpenAI
from pydantic import BaseModel

from schemas import PredictionRequest, PredictionResponse
from predictor import predict_occupancy, classify_congestion

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
        raise HTTPException(
            status_code=400,
            detail=str(error)
        )


class AnalysisRequest(BaseModel):
    simulationId: int
    zoneId: int
    capacity: int
    predictedOccupancy: int
    predictedCongestionLevel: str
    horizonMinutes: int = 10


@app.post("/analyze")
def analyze_simulation(request: AnalysisRequest):
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="OPENAI_API_KEY is not configured"
        )

    client = OpenAI(api_key=api_key)

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a crowd safety analyst. "
                        "Provide concise, practical recommendations "
                        "for event organizers. Do not claim certainty "
                        "or invent facts not present in the input."
                    )
                },
                {
                    "role": "user",
                    "content": (
                        f"Analyze this crowd prediction: "
                        f"simulation ID={request.simulationId}, "
                        f"zone ID={request.zoneId}, "
                        f"capacity={request.capacity}, "
                        f"predicted occupancy={request.predictedOccupancy}, "
                        f"predicted congestion={request.predictedCongestionLevel}, "
                        f"horizon={request.horizonMinutes} minutes. "
                        "Return a short analysis and actionable recommendations."
                    )
                }
            ],
            temperature=0.3
        )

        return {
            "simulationId": request.simulationId,
            "zoneId": request.zoneId,
            "analysis": response.choices[0].message.content
        }

    except Exception as error:
        raise HTTPException(
            status_code=502,
            detail=f"OpenAI analysis failed: {str(error)}"
        )

