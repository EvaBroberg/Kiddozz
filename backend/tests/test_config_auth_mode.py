"""Unit tests for AUTH_MODE configuration setting."""

import logging
from unittest.mock import patch

import pytest

from app.core.config import Settings


class TestAuthModeConfig:
    """Test AUTH_MODE configuration validation and defaults."""

    def test_auth_mode_defaults_to_dev_when_unset(self):
        """Test that AUTH_MODE defaults to DEV when environment variable is unset."""
        # Test the validator directly with None
        result = Settings.validate_auth_mode(None)
        assert result == "DEV"

    def test_auth_mode_invite_when_set(self):
        """Test that AUTH_MODE is set to INVITE when environment variable is INVITE."""
        # Test the validator directly
        result = Settings.validate_auth_mode("INVITE")
        assert result == "INVITE"
        
        # Test via Settings instance with env var
        import os
        with patch.dict(os.environ, {"AUTH_MODE": "INVITE"}):
            # Create new instance to pick up env var
            settings = Settings()
            assert settings.auth_mode == "INVITE"

    def test_auth_mode_case_insensitive(self):
        """Test that AUTH_MODE is case-insensitive."""
        test_cases = [
            ("invite", "INVITE"),
            ("Invite", "INVITE"),
            ("INVITE", "INVITE"),
            ("dev", "DEV"),
            ("Dev", "DEV"),
            ("DEV", "DEV"),
        ]
        
        for input_value, expected in test_cases:
            result = Settings.validate_auth_mode(input_value)
            assert result == expected, f"Failed for input: {input_value}"

    def test_auth_mode_defaults_to_dev_on_invalid_value(self, caplog):
        """Test that AUTH_MODE defaults to DEV and logs warning on invalid value."""
        with caplog.at_level(logging.WARNING):
            result = Settings.validate_auth_mode("garbage")
            
            # Should default to DEV
            assert result == "DEV"
            
            # Should log a warning
            assert "Invalid AUTH_MODE value" in caplog.text
            assert "garbage" in caplog.text
            assert "Defaulting to DEV" in caplog.text

    def test_auth_mode_handles_whitespace(self):
        """Test that AUTH_MODE handles whitespace correctly."""
        result = Settings.validate_auth_mode("  invite  ")
        assert result == "INVITE"

