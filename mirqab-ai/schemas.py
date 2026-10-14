
from typing import List

from pydantic import BaseModel, Field, model_validator


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

    @model_validator(mode="after")
    def validate_snapshots(self):
        minutes = [snapshot.minute for snapshot in self.recentSnapshots]

        if len(minutes) != len(set(minutes)):
            raise ValueError(
                "Snapshot minutes must be unique."
            )

        return self


class PredictionResponse(BaseModel):
    zoneId: int
    predictedOccupancy: int = Field(ge=0)
    predictedCongestionLevel: str
    horizonMinutes: int = Field(gt=0)
