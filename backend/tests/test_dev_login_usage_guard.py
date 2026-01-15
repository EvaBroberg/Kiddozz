from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Match:
    file: str
    line_no: int
    line: str


def _scan_file(path: Path, needles: list[str]) -> list[Match]:
    matches: list[Match] = []
    # Use utf-8 with replacement to avoid test flaking on unexpected encodings
    text = path.read_text(encoding="utf-8", errors="replace")
    for idx, line in enumerate(text.splitlines(), start=1):
        if any(needle in line for needle in needles):
            matches.append(Match(file=str(path), line_no=idx, line=line.rstrip("\n")))
    return matches


def test_dev_auth_shortcuts_not_used_outside_gating_tests() -> None:
    """
    Guardrail: prevent dev-only auth shortcuts from creeping back into tests.
    """

    tests_dir = Path(__file__).resolve().parent
    this_file = str(Path(__file__).resolve())

    # Substrings we want to ban in test modules.
    # Avoid embedding the banned substrings directly in this file.
    dev_login = "dev" + "-login"
    switch_role = "switch" + "-role"
    test_token = "test" + "-token"

    banned_needles = [
        dev_login,
        "/auth/" + dev_login,
        "/api/v1/auth/" + dev_login,
        switch_role,
        "/api/v1/auth/" + switch_role,
        test_token,
        "/api/v1/auth/" + test_token,
    ]

    offenders: list[Match] = []
    for path in sorted(tests_dir.glob("*.py")):
        resolved = str(path.resolve())
        if resolved == this_file:
            continue
        offenders.extend(_scan_file(path, banned_needles))

    if offenders:
        formatted = "\n".join(
            f"{m.file}:{m.line_no}: {m.line}" for m in offenders[:50]
        )
        raise AssertionError(
            "Dev auth shortcut usage detected outside allowlisted gating tests.\n"
            "Remove the usage or move the assertion into backend/tests/test_dev_auth_gating.py.\n\n"
            f"First {min(50, len(offenders))} match(es):\n{formatted}\n"
        )

