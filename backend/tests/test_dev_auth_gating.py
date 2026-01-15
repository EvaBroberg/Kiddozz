from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _set_env(monkeypatch, *, environment: str, app_env: str):
    monkeypatch.setenv("ENVIRONMENT", environment)
    monkeypatch.setenv("APP_ENV", app_env)


class TestDevAuthGating:
    def test_prod_env_blocks_dev_auth_endpoints(self, monkeypatch):
        _set_env(monkeypatch, environment="production", app_env="production")
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "production"
            mock_settings.app_env = "production"

            r1 = client.post("/api/v1/auth/switch-role?role=educator")
            r2 = client.post(
                "/api/v1/auth/test-token", json={"role": "educator", "user_id": 1}
            )

            assert r1.status_code == 404
            assert r2.status_code == 404

    def test_staging_env_blocks_dev_auth_endpoints(self, monkeypatch):
        _set_env(monkeypatch, environment="staging", app_env="staging")
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "staging"
            mock_settings.app_env = "staging"

            r1 = client.post("/api/v1/auth/switch-role?role=educator")
            r2 = client.post(
                "/api/v1/auth/test-token", json={"role": "educator", "user_id": 1}
            )

            assert r1.status_code == 404
            assert r2.status_code == 404

    def test_dev_local_allows_dev_auth_endpoints(self, monkeypatch):
        _set_env(monkeypatch, environment="development", app_env="local")
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "development"
            mock_settings.app_env = "local"

            r1 = client.post("/api/v1/auth/switch-role?role=educator")
            r2 = client.post(
                "/api/v1/auth/test-token", json={"role": "educator", "user_id": 1}
            )

            assert r1.status_code == 200
            assert "access_token" in r1.json()

            assert r2.status_code == 200
            assert "access_token" in r2.json()
