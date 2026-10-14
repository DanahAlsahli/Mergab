
from schemas import SnapshotInput


def predict_occupancy(
        snapshots: list[SnapshotInput],
        horizon_minutes: int = 10
) -> int:
    """Estimate future occupancy from the two latest snapshots."""

    if not snapshots:
        raise ValueError("At least one snapshot is required.")

    if horizon_minutes <= 0:
        raise ValueError("Prediction horizon must be positive.")

    ordered = sorted(snapshots, key=lambda snapshot: snapshot.minute)
    minutes = [snapshot.minute for snapshot in ordered]

    if len(minutes) != len(set(minutes)):
        raise ValueError("Snapshot minutes must be unique.")

    latest = ordered[-1]

    if len(ordered) == 1:
        return latest.occupancy

    previous = ordered[-2]
    elapsed_minutes = latest.minute - previous.minute

    if elapsed_minutes <= 0:
        raise ValueError(
            "Latest snapshots must have increasing minutes."
        )

    visitors_per_minute = (
                                  latest.occupancy - previous.occupancy
                          ) / elapsed_minutes

    predicted = latest.occupancy + (
            visitors_per_minute * horizon_minutes
    )

    return max(0, round(predicted))


def classify_congestion(
        predicted_occupancy: int,
        capacity: int
) -> str:
    """Classify predicted occupancy relative to zone capacity."""

    if capacity <= 0:
        raise ValueError("Capacity must be positive.")

    if predicted_occupancy < 0:
        raise ValueError("Predicted occupancy cannot be negative.")

    occupancy_ratio = predicted_occupancy / capacity

    if occupancy_ratio < 0.50:
        return "LOW"
    elif occupancy_ratio < 0.80:
        return "MEDIUM"
    else:
        return "HIGH"
