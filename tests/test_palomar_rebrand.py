from __future__ import annotations

import json
import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).parents[1]
HISTORICAL_PREFIXES = ("docs/releases/", "docs/acceptance-")
THEME_MIGRATION_FILES = {
    "android/app/src/main/java/net/kaltner/palomar/TokenStore.kt",
    "android/app/src/test/java/net/kaltner/palomar/PalomarConnectionTest.kt",
    "docs/themes.md",
    "web/public/assets/theme-startup.js",
    "web/src/storage-ui.test.ts",
    "web/src/storage.ts",
}


class PalomarRebrandTests(unittest.TestCase):
    def tracked_paths(self) -> list[Path]:
        result = subprocess.run(
            ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
            cwd=ROOT,
            check=True,
            capture_output=True,
            text=True,
        )
        return [path for line in result.stdout.splitlines() if line if (path := ROOT / line).exists()]

    def current_text(self) -> dict[str, str]:
        content: dict[str, str] = {}
        for path in self.tracked_paths():
            relative = path.relative_to(ROOT).as_posix()
            if relative.startswith(HISTORICAL_PREFIXES):
                continue
            try:
                content[relative] = path.read_text(encoding="utf-8")
            except (UnicodeDecodeError, IsADirectoryError):
                pass
        return content

    def test_current_tree_contains_no_old_product_identifiers(self) -> None:
        failures: list[str] = []
        for relative, text in self.current_text().items():
            old_name = "fore" + "man"
            intentional_theme_migration = (
                relative in THEME_MIGRATION_FILES
                or relative.startswith("web/dist/assets/")
            )
            if old_name in text.casefold() and not intentional_theme_migration:
                failures.append(relative)
            if ("mkaltner/" + "palomar") in text.casefold():
                failures.append(f"{relative} (personal repository owner)")
            if ("net.kaltner." + old_name) in text.casefold():
                failures.append(f"{relative} (old Android ID)")
            for prefix in tuple("fm" + suffix for suffix in ("t_", "c_", "u_", "p_")):
                if prefix in text:
                    failures.append(f"{relative} (old internal prefix {prefix})")
        for path in self.tracked_paths():
            relative = path.relative_to(ROOT).as_posix()
            if not relative.startswith(HISTORICAL_PREFIXES) and ("fore" + "man") in relative.casefold():
                failures.append(f"{relative} (path)")
        self.assertEqual([], sorted(set(failures)))

    def test_authoritative_identity_and_versions_are_palomar_2(self) -> None:
        release = (ROOT / "palomar-release.properties").read_text(encoding="utf-8")
        web_package = json.loads((ROOT / "web/package.json").read_text(encoding="utf-8"))
        bridge_package = json.loads((ROOT / "linux/claude_bridge/package.json").read_text(encoding="utf-8"))
        android_build = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        android_manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

        self.assertIn("palomarVersion=2.1.0", release)
        self.assertEqual("palomar-web", web_package["name"])
        self.assertEqual("2.1.0", web_package["version"])
        self.assertEqual("2.1.0", bridge_package["version"])
        self.assertIn('namespace = "net.kaltner.palomar"', android_build)
        self.assertIn('applicationId = "net.kaltner.palomar"', android_build)
        self.assertIn('@string/app_name', android_manifest)
        self.assertIn('@mipmap/ic_launcher', android_manifest)

    def test_clean_break_paths_and_release_names_are_enforced(self) -> None:
        required = [
            "linux/palomar",
            "linux/palomar.service",
            "linux/palomar-update-recovery.service",
            "linux/palomar_service.py",
            "linux/palomar_updater.py",
            "linux/completions/palomar.bash",
            "linux/completions/_palomar",
            "linux/completions/palomar.fish",
            "scripts/install-palomar.sh",
            "palomar-release.properties",
            "web/public/palomar-mark.svg",
            "web/public/palomar-mark-16px.svg",
            "web/public/palomar-app-icon.svg",
            "web/public/manifest.webmanifest",
            "docs/brand/palomar-mark.svg",
            "docs/brand/palomar-hero-wide.svg",
            "docs/brand/palomar-social-preview.png",
        ]
        for relative in required:
            self.assertTrue((ROOT / relative).is_file(), relative)
        release_workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
        bootstrap = (ROOT / "scripts/install-palomar.sh").read_text(encoding="utf-8")
        for name in (
            "palomar-${RELEASE_TAG}.apk",
            "palomar-linux-${RELEASE_TAG}.tar.gz",
            "palomar-SHA256SUMS",
            "palomar-SHA256SUMS.sig",
            "palomar-release-cert.pem",
        ):
            self.assertIn(name, release_workflow if "${RELEASE_TAG}" in name else bootstrap)
        self.assertIn("https://api.github.com/repos/kaltner-net/palomar/releases", bootstrap)


if __name__ == "__main__":
    unittest.main()
