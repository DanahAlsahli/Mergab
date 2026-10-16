
import pytest
from fastapi.testclient import TestClient

import main
from main import app

client = TestClient(app)

VALID_REQUEST = {
    "simulationId": 1,
    "zoneId": 1,
    "capacity": 1000,
    "predictedOccupancy": 500,
    "predictedCongestionLevel": "MEDIUM",
    "horizonMinutes": 10,
}


def test_analyze_without_api_key(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)

    response = client.post("/analyze", json=VALID_REQUEST)

    assert response.status_code == 503
    assert response.json()["detail"] == "AI analysis is not configured."


def test_analyze_success(monkeypatch):
    class FakeMessage:
        content = "Analysis completed successfully."

    class FakeChoice:
        message = FakeMessage()

    class FakeResponse:
        choices = [FakeChoice()]

    class FakeCompletions:
        def create(self, **kwargs):
            return FakeResponse()

    class FakeChat:
        completions = FakeCompletions()

    class FakeClient:
        chat = FakeChat()

    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setattr(main, "OpenAI", lambda **kwargs: FakeClient())

    response = client.post("/analyze", json=VALID_REQUEST)

    assert response.status_code == 200
    assert response.json()["analysis"] == "Analysis completed successfully."


def test_analyze_empty_response(monkeypatch):
    class FakeMessage:
        content = "   "

    class FakeChoice:
        message = FakeMessage()

    class FakeResponse:
        choices = [FakeChoice()]

    class FakeCompletions:
        def create(self, **kwargs):
            return FakeResponse()

    class FakeChat:
        completions = FakeCompletions()

    class FakeClient:
        chat = FakeChat()

    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setattr(main, "OpenAI", lambda **kwargs: FakeClient())

    response = client.post("/analyze", json=VALID_REQUEST)

    assert response.status_code == 502
    assert response.json()["detail"] == "AI analysis returned an empty response."


def test_analyze_openai_failure(monkeypatch):
    class FakeCompletions:
        def create(self, **kwargs):
            raise RuntimeError("Simulated OpenAI failure")

    class FakeChat:
        completions = FakeCompletions()

    class FakeClient:
        chat = FakeChat()

    monkeypatch.setenv("OPENAI_API_KEY", "test-key")
    monkeypatch.setattr(main, "OpenAI", lambda **kwargs: FakeClient())

    response = client.post("/analyze", json=VALID_REQUEST)

    assert response.status_code == 502
    assert response.json()["detail"] == (
        "AI analysis is temporarily unavailable. Please try again."
    )
