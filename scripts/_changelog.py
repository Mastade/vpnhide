"""Shared helpers for changelog manipulation.

Used by `changelog-add.py` (append entries) and `update-version.py`
(rotate top-level into history when VERSION advances).

Source of truth: `lsposed/app/src/main/assets/changelog.json` (bilingual,
full history).

Two generated markdown artifacts (en only, overwritten on every script
run — never edited by hand):

* `CHANGELOG.md` at the repo root — full history, Keep a Changelog
  convention. Human-facing, linked from the GitHub repo page.
* `update-json/changelog.md` — the last MD_RECENT_VERSIONS sections
  only. Served at a stable URL referenced from module update-json
  files; Magisk/KSU fetches it and displays it in the update popup.
"""

from __future__ import annotations

import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
JSON_PATH = REPO_ROOT / "lsposed/app/src/main/assets/changelog.json"
FULL_MD_PATH = REPO_ROOT / "CHANGELOG.md"
SHORT_MD_PATH = REPO_ROOT / "update-json/changelog.md"

VALID_TYPES = ("added", "changed", "fixed", "removed", "deprecated", "security")
MD_RECENT_VERSIONS = 5

_KEEP_A_CHANGELOG_HEADER = """# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

"""


def load_json() -> dict:
    return json.loads(JSON_PATH.read_text(encoding="utf-8"))


def save_json(data: dict) -> None:
    JSON_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def _render_entry(entry: dict, out: list[str]) -> None:
    out.append(f"## v{entry['version']}")
    out.append("")
    sections_by_type = {s["type"]: s for s in entry.get("sections", [])}
    for type_ in VALID_TYPES:
        section = sections_by_type.get(type_)
        if not section or not section.get("items"):
            continue
        out.append(f"### {type_.title()}")
        for item in section["items"]:
            out.append(f"- {item['en']}")
        out.append("")


def render_full_md(data: dict) -> str:
    """Full history, with Keep a Changelog header on top."""
    entries = [data] + list(data.get("history", []))
    out: list[str] = []
    for entry in entries:
        _render_entry(entry, out)
    return _KEEP_A_CHANGELOG_HEADER + "\n".join(out).rstrip() + "\n"


def render_short_md(data: dict) -> str:
    """Last MD_RECENT_VERSIONS only, no preamble. For Magisk/KSU popup."""
    entries = ([data] + list(data.get("history", [])))[:MD_RECENT_VERSIONS]
    out: list[str] = []
    for entry in entries:
        _render_entry(entry, out)
    return "\n".join(out).rstrip() + "\n"


def write_md(data: dict) -> None:
    FULL_MD_PATH.write_text(render_full_md(data), encoding="utf-8")
    SHORT_MD_PATH.write_text(render_short_md(data), encoding="utf-8")


def append_entry(data: dict, type_: str, en: str, ru: str) -> None:
    """Add an entry to the top-level (upcoming) version's sections."""
    if type_ not in VALID_TYPES:
        raise ValueError(f"invalid type {type_!r}; valid: {', '.join(VALID_TYPES)}")
    sections = data.setdefault("sections", [])
    section = next((s for s in sections if s["type"] == type_), None)
    if section is None:
        section = {"type": type_, "items": []}
        sections.append(section)
    section["items"].append({"en": en, "ru": ru})


def rotate_for_version(data: dict, new_version: str) -> bool:
    """If top-level.version != new_version, push it to history[0] and
    create a new empty top-level for new_version. Returns True if rotated.
    """
    current_version = data.get("version")
    if current_version == new_version:
        return False
    rotated = {
        "version": current_version,
        "sections": data.get("sections", []),
    }
    history = data.setdefault("history", [])
    history.insert(0, rotated)
    data["version"] = new_version
    data["sections"] = []
    return True
