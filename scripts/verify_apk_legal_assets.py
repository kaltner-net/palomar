#!/usr/bin/env python3
"""Verify that a Foreman APK carries its first-party legal files."""

from __future__ import annotations

import argparse
import zipfile
from pathlib import Path


REQUIRED_LEGAL_ASSETS = frozenset(
    {
        "assets/LICENSE",
        "assets/THIRD_PARTY_NOTICES.md",
    }
)


def missing_legal_assets(apk: Path) -> set[str]:
    """Return required legal assets absent from an APK ZIP."""

    with zipfile.ZipFile(apk) as archive:
        return set(REQUIRED_LEGAL_ASSETS - set(archive.namelist()))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path, help="APK to inspect")
    args = parser.parse_args()

    try:
        missing = missing_legal_assets(args.apk)
    except (FileNotFoundError, zipfile.BadZipFile) as error:
        raise SystemExit(f"cannot inspect release APK: {error}") from error
    if missing:
        raise SystemExit(f"release APK is missing {', '.join(sorted(missing))}")

    print("APK includes Foreman license and third-party notices")


if __name__ == "__main__":
    main()
