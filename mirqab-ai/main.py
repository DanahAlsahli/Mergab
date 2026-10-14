from fastapi import FastAPI, HTTPException

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
