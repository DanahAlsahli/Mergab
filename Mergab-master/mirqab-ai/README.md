# Mirqab AI — Congestion Prediction Service

## Overview
A FastAPI service that predicts zone occupancy 10 minutes into the future and classifies congestion as LOW, MEDIUM, or HIGH.

## Technologies
- Python
- FastAPI
- Pydantic
- Uvicorn
- Pytest

## Setup

Create and activate a virtual environment:

```powershell
py -m venv .venv
.venv\Scripts\activate
```

Install dependencies:

```powershell
pip install -r requirements.txt
```

## Run the service

```powershell
python -m uvicorn main:app --reload
```

API documentation:
http://127.0.0.1:8000/docs

Health check:
GET /health

Prediction endpoint:
POST /predict

## Example request

```json
{
  "simulationId": 1,
  "zoneId": 2,
  "capacity": 1000,
  "predictionHorizonMinutes": 10,
  "recentSnapshots": [
    {
      "minute": 28,
      "occupancy": 790,
      "queueLength": 95
    },
    {
      "minute": 29,
      "occupancy": 820,
      "queueLength": 110
    },
    {
      "minute": 30,
      "occupancy": 850,
      "queueLength": 120
    }
  ]
}
```

## Example response

```json
{
  "zoneId": 2,
  "predictedOccupancy": 1150,
  "predictedCongestionLevel": "HIGH",
  "horizonMinutes": 10
}
```

## Run tests

```powershell
python -m pytest -v
```

## Current limitations
- The predictor uses the trend between the two latest snapshots; it is a baseline estimator, not a trained machine-learning model.
- Zone capacity must be supplied in each prediction request.
- Prediction accuracy has not yet been evaluated against historical data.
