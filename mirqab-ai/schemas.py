from pydantic import BaseModel, Field
from typing import List


class SnapshotInput(BaseModel):
    minute: int = Field(ge=0)
    occupancy: int = Field(ge=0)
    queueLength: int = Field(ge=0)


class PredictionRequest(BaseModel):
    simulationId: int = Field(gt=0)
    zoneId: int = Field(gt=0)
    capacity: int = Field(gt=0)
    predictionHorizonMinutes: int = Field(default=10, gt=0)
    recentSnapshots: List[SnapshotInput] = Field(min_length=1)


class PredictionResponse(BaseModel):
    zoneId: int
    predictedOccupancy: int
    predictedCongestionLevel: str
    horizonMinutes: int
