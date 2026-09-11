from __future__ import annotations

import os
import shutil
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).parents[1]


class PalomarCliTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.home = Path(self.temporary.name)
        self.fake_bin = self.home / "fake-bin"
        self.fake_bin.mkdir()
        self.log = self.home / "commands.log"
        self._executable(
            self.fake_bin / "systemctl",
            '#!/bin/sh\nprintf "systemctl %s\\n" "$*" >> "$PALOMAR_TEST_COMMAND_LOG"\n',
        )
        self._executable(
            self.fake_bin / "journalctl",
            '#!/bin/sh\nprintf "journalctl %s\\n" "$*" >> "$PALOMAR_TEST_COMMAND_LOG"\n',
        )
        self._executable(
            self.fake_bin / "python3",
            '#!/bin/sh\nprintf "python3 %s\\n" "$*" >> "$PALOMAR_TEST_COMMAND_LOG"\n',
        )
        self.environment = {
            **os.environ,
            "HOME": str(self.home),
            "PATH": f"{self.fake_bin}:/usr/bin:/bin",
            "PALOMAR_INSTALL_DIR": str(self.home / ".local/share/palomar"),
            "PALOMAR_TEST_COMMAND_LOG": str(self.log),
            "XDG_RUNTIME_DIR": str(self.home / "runtime"),
        }

    def tearDown(self) -> None:
        self.temporary.cleanup()

    @staticmethod
    def _executable(path: Path, body: str) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding="utf-8")
        path.chmod(path.stat().st_mode | stat.S_IXUSR)

    def run_cli(self, *arguments: str, launcher: Path | None = None) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [str(launcher or ROOT / "linux/palomar"), *arguments],
            env=self.environment,
            cwd=ROOT,
            check=False,
            capture_output=True,
            text=True,
        )

    def test_service_lifecycle_commands_target_only_palomar(self) -> None:
        for command in ("start", "stop", "status"):
            with self.subTest(command=command):
                result = self.run_cli(command)
                self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(
            self.log.read_text(encoding="utf-8").splitlines(),
            [
                "systemctl --user start palomar.service",
                "systemctl --user stop palomar.service",
                "systemctl --user status palomar.service --no-pager",
            ],
        )

    def test_update_delegates_to_installed_palomar_runtime(self) -> None:
        install_dir = Path(self.environment["PALOMAR_INSTALL_DIR"])
        install_dir.mkdir(parents=True)
        (install_dir / "update_cli.py").write_text("", encoding="utf-8")

        result = self.run_cli("update", "--check")

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(f"python3 {install_dir}/update_cli.py --check", self.log.read_text(encoding="utf-8"))

    def test_source_install_command_runs_the_adjacent_transactional_installer(self) -> None:
        source = self.home / "source"
        (source / "linux").mkdir(parents=True)
        launcher = source / "linux/palomar"
        shutil.copy2(ROOT / "linux/palomar", launcher)
        self._executable(
            source / "install.sh",
            '#!/bin/sh\nprintf "source installer\\n" >> "$PALOMAR_TEST_COMMAND_LOG"\n',
        )

        result = self.run_cli("install", launcher=launcher)

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("source installer", self.log.read_text(encoding="utf-8"))

    def test_uninstall_purge_removes_only_palomar_paths(self) -> None:
        install_dir = Path(self.environment["PALOMAR_INSTALL_DIR"])
        launcher = self.home / ".local/bin/palomar"
        unit = self.home / ".config/systemd/user/palomar.service"
        recovery = self.home / ".config/systemd/user/palomar-update-recovery.service"
        helper = self.home / ".local/libexec/palomar-updater"
        completions = (
            self.home / ".local/share/bash-completion/completions/palomar",
            self.home / ".local/share/zsh/site-functions/_palomar",
            self.home / ".local/share/fish/vendor_completions.d/palomar.fish",
        )
        for path in (install_dir, self.home / ".config/palomar", self.home / ".local/state/palomar", self.home / ".cache/palomar", self.home / "runtime/palomar"):
            path.mkdir(parents=True, exist_ok=True)
            (path / "sentinel").write_text("Palomar", encoding="utf-8")
        for path in (launcher, unit, recovery, helper, *completions):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("Palomar", encoding="utf-8")
        shutil.copy2(ROOT / "linux/palomar_uninstall", install_dir / "palomar_uninstall")
        (install_dir / "palomar_uninstall").chmod(0o755)
        shutil.copy2(ROOT / "linux/palomar", launcher)
        launcher.chmod(0o755)
        unrelated = self.home / ".config/unrelated"
        unrelated.mkdir(parents=True)
        (unrelated / "keep").write_text("keep", encoding="utf-8")

        result = self.run_cli("uninstall", "--purge", "--yes", launcher=launcher)

        self.assertEqual(result.returncode, 0, result.stderr)
        for path in (install_dir, self.home / ".config/palomar", self.home / ".local/state/palomar", self.home / ".cache/palomar", self.home / "runtime/palomar"):
            self.assertFalse(path.exists(), path)
        for path in (launcher, unit, recovery, helper, *completions):
            self.assertFalse(path.exists(), path)
        self.assertTrue((unrelated / "keep").is_file())


if __name__ == "__main__":
    unittest.main()
