# Mirqab AI API Contract

## Base URL (local development)
http://127.0.0.1:8000

## Health Check
GET /health

Successful response: HTTP 200

```json
{
  "status": "healthy"
}
```

## Predict Congestion
POST /predict
Content-Type: application/json

### Request

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

### Successful response

HTTP 200

```json
{
  "zoneId": 2,
  "predictedOccupancy": 1150,
  "predictedCongestionLevel": "HIGH",
  "horizonMinutes": 10
}
```

## Request fields

- simulationId: positive integer
- zoneId: positive integer
- capacity: positive integer representing the zone capacity
- predictionHorizonMinutes: positive integer; defaults to 10
- recentSnapshots: at least one snapshot
- minute: non-negative snapshot time
- occupancy: non-negative occupancy count
- queueLength: non-negative queue length

## Integration notes

- Spring Boot must supply the actual capacity for the requested zone.
- The response currently returns the zone ID, predicted occupancy, congestion level, and prediction horizon.
- The current predictor estimates occupancy using the two latest snapshots.
- queueLength is accepted but is not currently used in the prediction calculation.
- The prediction baseline has not been validated against historical data.
- The local URL is for development only. The deployed service URL must be configured for integration testing.
