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



def test_single_snapshot():
    snapshot = SnapshotInput(minute=5, occupancy=200, queueLength=10)
    assert predict_occupancy([snapshot], horizon_minutes=10) == 200


def test_empty_snapshots():
    import pytest

    with pytest.raises(ValueError):
        predict_occupancy([], horizon_minutes=10)


def test_prediction_decreases():
    snapshots = [
        SnapshotInput(minute=4, occupancy=200, queueLength=20),
        SnapshotInput(minute=5, occupancy=150, queueLength=15),
    ]

    assert predict_occupancy(snapshots, horizon_minutes=10) == 0


def test_invalid_capacity():
    import pytest

    with pytest.raises(ValueError):
        classify_congestion(100, 0)


def test_congestion_at_capacity():
    assert classify_congestion(1000, 1000) == "HIGH"


def test_congestion_above_capacity():
    assert classify_congestion(1200, 1000) == "HIGH"

