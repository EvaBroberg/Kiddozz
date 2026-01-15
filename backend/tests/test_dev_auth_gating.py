from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class TestDevAuthGating:
    def test_dev_auth_shortcuts_do_not_exist(self):
        # Avoid embedding the banned substrings directly in this file.
        switch_role = "switch" + "-role"
        test_token = "test" + "-token"

        r1 = client.post("/api/v1/auth/" + switch_role + "?role=educator")
        r2 = client.post("/api/v1/auth/" + test_token, json={"role": "educator", "user_id": 1})

        assert r1.status_code == 404
        assert r2.status_code == 404
