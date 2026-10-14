from schemas import SnapshotInput
from predictor import predict_occupancy, classify_congestion


def test_predict_occupancy():
    snapshots = [
        SnapshotInput(minute=28, occupancy=790, queueLength=95),
        SnapshotInput(minute=29, occupancy=820, queueLength=110),
        SnapshotInput(minute=30, occupancy=850, queueLength=120),
    ]

    result = predict_occupancy(snapshots, horizon_minutes=10)

    assert result == 1150


def test_low_congestion():
    assert classify_congestion(300, 1000) == "LOW"


def test_medium_congestion():
    assert classify_congestion(600, 1000) == "MEDIUM"


def test_high_congestion():
    assert classify_congestion(850, 1000) == "HIGH"


def test_zero_occupancy():
    assert classify_congestion(0, 1000) == "LOW"
