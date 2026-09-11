#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
config_dir="$HOME/.config/palomar"
state_dir="$HOME/.local/state/palomar"
install_dir="$HOME/.local/share/palomar"
install_parent="$HOME/.local/share"
bin_dir="$HOME/.local/bin"
libexec_dir="$HOME/.local/libexec"
unit_dir="$HOME/.config/systemd/user"
bash_completion_dir="$HOME/.local/share/bash-completion/completions"
zsh_completion_dir="$HOME/.local/share/zsh/site-functions"
fish_completion_dir="$HOME/.local/share/fish/vendor_completions.d"
config_file="$config_dir/palomar.env"
launcher_file="$bin_dir/palomar"
unit_file="$unit_dir/palomar.service"
recovery_unit_file="$unit_dir/palomar-update-recovery.service"
helper_file="$libexec_dir/palomar-updater"
pinned_version="$(sed -n 's/^websockets==//p' "$project_dir/requirements.txt")"
staging_dir=""
backup_dir=""
rollback_required=0
had_launcher=0
had_unit=0
had_recovery_unit=0
had_helper=0
claude_runtime_ready=0
claude_needs_sdk_install=0
claude_sdk_source=""
required_claude_sdk_version=""

configured_value() {
  local key="$1"
  local value=""
  if [[ -f "$config_file" ]]; then
    value="$(awk -v key="$key" '
      index($0, "=") {
        name = substr($0, 1, index($0, "=") - 1)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
        if (name == key) {
          value = substr($0, index($0, "=") + 1)
          gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        }
      }
      END { print value }
    ' "$config_file")"
    value="${value#\"}"
    value="${value%\"}"
    value="${value#\'}"
    value="${value%\'}"
  fi
  printf '%s' "$value"
}

resolve_executable() {
  local name="$1"
  local configured="${2:-}"
  local candidate=""
  if [[ -n "$configured" ]]; then
    if [[ "$configured" == */* ]]; then
      candidate="$configured"
    else
      candidate="$(command -v -- "$configured" 2>/dev/null || true)"
    fi
  else
    candidate="$(command -v -- "$name" 2>/dev/null || true)"
  fi
  if [[ -n "$candidate" && -f "$candidate" && -x "$candidate" ]]; then
    readlink -f -- "$candidate"
  fi
}

package_version() {
  python3 - "$1" <<'PY'
import json
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
try:
    value = json.loads(path.read_text(encoding="utf-8")).get("version", "")
except (OSError, json.JSONDecodeError):
    value = ""
print(value if isinstance(value, str) else "")
PY
}

cleanup() {
  status=$?
  set +e
  if [[ "$rollback_required" == 1 && -n "$backup_dir" && -d "$backup_dir/install" ]]; then
    systemctl --user stop palomar.service >/dev/null 2>&1
    rm -rf -- "$install_dir"
    mv -- "$backup_dir/install" "$install_dir"
    if [[ "$had_launcher" == 1 ]]; then
      install -m 755 "$backup_dir/palomar" "$launcher_file"
    else
      rm -f -- "$launcher_file"
    fi
    if [[ "$had_unit" == 1 ]]; then
      install -m 644 "$backup_dir/palomar.service" "$unit_file"
    else
      rm -f -- "$unit_file"
    fi
    if [[ "$had_recovery_unit" == 1 ]]; then
      install -m 644 "$backup_dir/palomar-update-recovery.service" "$recovery_unit_file"
    else
      systemctl --user disable palomar-update-recovery.service >/dev/null 2>&1
      rm -f -- "$recovery_unit_file"
    fi
    if [[ "$had_helper" == 1 ]]; then
      install -m 755 "$backup_dir/palomar-updater" "$helper_file"
    else
      rm -f -- "$helper_file"
    fi
    systemctl --user daemon-reload >/dev/null 2>&1
    systemctl --user restart palomar.service >/dev/null 2>&1
  fi
  if [[ -n "$staging_dir" && -d "$staging_dir" ]]; then
    rm -rf -- "$staging_dir"
  fi
  if [[ -n "$backup_dir" && -d "$backup_dir" ]]; then
    rm -rf -- "$backup_dir"
  fi
  exit "$status"
}
trap cleanup EXIT

command -v python3 >/dev/null || {
  echo "python3 is required" >&2
  exit 1
}
command -v openssl >/dev/null || {
  echo "openssl is required for signed Palomar updates" >&2
  exit 1
}
command -v systemd-run >/dev/null || {
  echo "systemd-run is required for recoverable Palomar updates" >&2
  exit 1
}
python3 -c 'import sys; raise SystemExit(sys.version_info < (3, 10))' || {
  echo "Python 3.10 or newer is required" >&2
  exit 1
}
[[ -d "$project_dir/linux/vendor/websockets" ]] || {
  echo "Vendored Python dependencies are missing from the install payload" >&2
  exit 1
}
[[ -f "$project_dir/web/dist/index.html" ]] || {
  echo "Prebuilt web assets are missing; maintainers must run npm ci && npm run build in web/" >&2
  exit 1
}
compgen -G "$project_dir/web/dist/assets/*" >/dev/null || {
  echo "Prebuilt web assets are incomplete" >&2
  exit 1
}
[[ -n "$pinned_version" ]] || {
  echo "requirements.txt must pin websockets with ==" >&2
  exit 1
}
codex_executable="$(resolve_executable codex "$(configured_value PALOMAR_CODEX_EXECUTABLE)")"
claude_executable="$(resolve_executable claude "$(configured_value PALOMAR_CLAUDE_EXECUTABLE)")"
if [[ -z "$codex_executable" && -z "$claude_executable" ]]; then
  echo "Palomar requires at least one supported provider CLI: Codex (codex) or Claude Code (claude)." >&2
  echo "Install either CLI and ensure its executable is on PATH, then rerun ./install.sh." >&2
  exit 1
fi

node_executable=""
npm_executable=""
if [[ -n "$claude_executable" ]]; then
  required_claude_sdk_version="$(python3 - "$project_dir/linux/claude_bridge/package.json" <<'PY'
import json
import pathlib
import sys

try:
    package = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
    version = package.get("dependencies", {}).get("@anthropic-ai/claude-agent-sdk", "")
except (OSError, json.JSONDecodeError):
    version = ""
print(version if isinstance(version, str) else "")
PY
)"
  [[ -n "$required_claude_sdk_version" ]] || {
    echo "Claude bridge package.json must pin @anthropic-ai/claude-agent-sdk" >&2
    exit 1
  }
  node_executable="$(resolve_executable node "$(configured_value PALOMAR_NODE_EXECUTABLE)")"
  if [[ -n "$node_executable" ]]; then
    node_major="$($node_executable --version 2>/dev/null | sed -n 's/^v\{0,1\}\([0-9][0-9]*\).*/\1/p')"
    if [[ -z "$node_major" || "$node_major" -lt 20 ]]; then
      node_executable=""
    fi
  fi
  project_sdk="$project_dir/linux/claude_bridge/node_modules/@anthropic-ai/claude-agent-sdk/package.json"
  installed_sdk="$install_dir/claude_bridge/node_modules/@anthropic-ai/claude-agent-sdk/package.json"
  if [[ -n "$node_executable" ]] \
    && [[ "$(package_version "$project_sdk")" == "$required_claude_sdk_version" ]]; then
    claude_runtime_ready=1
    claude_sdk_source="project"
  elif [[ -n "$node_executable" ]] \
    && [[ "$(package_version "$installed_sdk")" == "$required_claude_sdk_version" ]]; then
    claude_runtime_ready=1
    claude_sdk_source="installed"
  elif [[ -n "$node_executable" ]]; then
    npm_executable="$(resolve_executable npm)"
    if [[ -n "$npm_executable" ]]; then
      claude_needs_sdk_install=1
    fi
  fi
  if [[ "$claude_runtime_ready" == 0 && "$claude_needs_sdk_install" == 0 ]]; then
    if [[ -z "$codex_executable" ]]; then
      echo "Claude Code is the only detected provider, but its Palomar runtime is incomplete." >&2
      if [[ -z "$node_executable" ]]; then
        echo "Install Node.js 20 or newer and ensure node is on PATH." >&2
      else
        echo "The pinned Claude Agent SDK is missing. Install npm, or run 'npm ci --omit=dev --ignore-scripts' in linux/claude_bridge, then rerun ./install.sh." >&2
      fi
      exit 1
    fi
    echo "Claude Code support will be unavailable: Node.js 20+ and the pinned Claude Agent SDK are required." >&2
  fi
fi

install -d -m 700 "$config_dir" "$state_dir"
install -d -m 755 "$install_parent" "$bin_dir" "$libexec_dir" "$unit_dir"

staging_dir="$(mktemp -d "$install_parent/.palomar-install.XXXXXX")"
install -m 755 "$project_dir/linux/palomar_service.py" "$staging_dir/palomar_service.py"
install -m 755 "$project_dir/linux/palomar_uninstall" "$staging_dir/palomar_uninstall"
install -m 644 "$project_dir/linux/codex.py" "$staging_dir/codex.py"
install -m 644 "$project_dir/linux/approvals.py" "$staging_dir/approvals.py"
install -m 644 "$project_dir/linux/inputs.py" "$staging_dir/inputs.py"
install -m 644 "$project_dir/linux/protocol.py" "$staging_dir/protocol.py"
install -m 644 "$project_dir/linux/state.py" "$staging_dir/state.py"
install -m 644 "$project_dir/linux/diagnostics.py" "$staging_dir/diagnostics.py"
install -m 644 "$project_dir/linux/claude_code.py" "$staging_dir/claude_code.py"
install -m 644 "$project_dir/linux/session_identity.py" "$staging_dir/session_identity.py"
install -m 644 "$project_dir/linux/release_updates.py" "$staging_dir/release_updates.py"
install -m 644 "$project_dir/linux/server_update.py" "$staging_dir/server_update.py"
install -m 644 "$project_dir/linux/update_cli.py" "$staging_dir/update_cli.py"
cp -a "$project_dir/linux/claude_bridge" "$staging_dir/claude_bridge"
rm -f -- \
  "$staging_dir/claude_bridge/bridge.test.mjs" \
  "$staging_dir/claude_bridge/test_fake_sdk.mjs"
if [[ -L "$staging_dir/claude_bridge/node_modules" ]]; then
  rm -f -- "$staging_dir/claude_bridge/node_modules"
fi
if [[ "$claude_sdk_source" == "installed" ]]; then
  rm -rf -- "$staging_dir/claude_bridge/node_modules"
  cp -a "$install_dir/claude_bridge/node_modules" \
    "$staging_dir/claude_bridge/node_modules"
fi
if [[ "$claude_needs_sdk_install" == 1 ]]; then
  echo "Preparing the pinned Claude Agent SDK in the staged install payload..."
  if (
    cd -- "$staging_dir/claude_bridge"
    "$npm_executable" ci --omit=dev --ignore-scripts
  ); then
    claude_runtime_ready=1
  elif [[ -z "$codex_executable" ]]; then
    echo "Claude Code is the only detected provider, and the pinned Claude Agent SDK could not be prepared." >&2
    echo "Check npm/network access or prepare linux/claude_bridge with 'npm ci --omit=dev --ignore-scripts', then rerun ./install.sh." >&2
    exit 1
  else
    echo "Claude Code support is unavailable because the pinned Agent SDK could not be prepared; Codex installation will continue." >&2
    rm -rf -- "$staging_dir/claude_bridge/node_modules"
  fi
fi
staged_sdk="$staging_dir/claude_bridge/node_modules/@anthropic-ai/claude-agent-sdk/package.json"
if [[ "$claude_runtime_ready" == 1 ]] \
  && [[ "$(package_version "$staged_sdk")" != "$required_claude_sdk_version" ]]; then
  echo "Claude Code runtime validation failed: the exact pinned Agent SDK is missing from the staged payload." >&2
  exit 1
fi
if [[ "$claude_runtime_ready" == 1 ]] && ! (
  cd -- "$staging_dir/claude_bridge"
  "$node_executable" --check bridge.mjs >/dev/null
  "$node_executable" --input-type=module -e \
    "await import('@anthropic-ai/claude-agent-sdk')" >/dev/null
); then
  if [[ -z "$codex_executable" ]]; then
    echo "Claude Code is the only detected provider, but the staged bridge or pinned Agent SDK cannot be loaded." >&2
    echo "Re-run 'npm ci --omit=dev --ignore-scripts' in linux/claude_bridge and retry installation." >&2
    exit 1
  fi
  echo "Claude Code support is unavailable because its staged bridge runtime could not be loaded; Codex installation will continue." >&2
  rm -rf -- "$staging_dir/claude_bridge/node_modules"
  claude_runtime_ready=0
fi
if [[ -n "$claude_executable" && "$claude_runtime_ready" == 0 ]]; then
  rm -rf -- "$staging_dir/claude_bridge/node_modules"
fi
install -m 644 "$project_dir/palomar-release.properties" "$staging_dir/palomar-release.properties"
cp -a "$project_dir/linux/vendor" "$staging_dir/vendor"
cp -a "$project_dir/web/dist" "$staging_dir/web"
if [[ -d "$install_dir/venv" ]]; then
  cp -a "$install_dir/venv" "$staging_dir/venv"
fi

python3 -m compileall -q \
  "$staging_dir/palomar_service.py" \
  "$staging_dir/codex.py" \
  "$staging_dir/approvals.py" \
  "$staging_dir/inputs.py" \
  "$staging_dir/protocol.py" \
  "$staging_dir/state.py" \
  "$staging_dir/diagnostics.py" \
  "$staging_dir/claude_code.py" \
  "$staging_dir/session_identity.py" \
  "$staging_dir/release_updates.py" \
  "$staging_dir/server_update.py" \
  "$staging_dir/update_cli.py" \
  "$staging_dir/vendor"
PALOMAR_STAGING_DIR="$staging_dir" \
PALOMAR_WEBSOCKETS_VERSION="$pinned_version" \
python3 -c '
import os, pathlib, sys
root = pathlib.Path(os.environ["PALOMAR_STAGING_DIR"])
sys.path.insert(0, str(root))
import codex
import websockets
assert websockets.__version__ == os.environ["PALOMAR_WEBSOCKETS_VERSION"]
assert pathlib.Path(websockets.__file__).is_relative_to(root / "vendor")
from websockets.asyncio.client import unix_connect
from websockets.asyncio.server import unix_serve
'
python3 "$staging_dir/palomar_service.py" --help >/dev/null

if [[ ! -e "$config_file" ]]; then
  {
    printf 'PALOMAR_HOST=0.0.0.0\n'
    printf 'PALOMAR_PORT=8765\n'
    printf 'PALOMAR_WEB_HOST=0.0.0.0\n'
    printf 'PALOMAR_WEB_PORT=8766\n'
    printf 'PALOMAR_REMOTE_RESTART=0\n'
    printf 'PALOMAR_REPOSITORY_ROOT=%s\n' "$HOME/projects"
    if [[ -n "$codex_executable" ]]; then
      printf 'PALOMAR_CODEX_EXECUTABLE=%s\n' "$codex_executable"
    fi
    if [[ -n "$claude_executable" ]]; then
      printf 'PALOMAR_CLAUDE_EXECUTABLE=%s\n' "$claude_executable"
    fi
    if [[ -n "$node_executable" ]]; then
      printf 'PALOMAR_NODE_EXECUTABLE=%s\n' "$node_executable"
    fi
  } >"$config_file"
  chmod 600 "$config_file"
fi

backup_dir="$(mktemp -d "$install_parent/.palomar-backup.XXXXXX")"
if [[ -d "$install_dir" ]]; then
  mv -- "$install_dir" "$backup_dir/install"
else
  mkdir "$backup_dir/install"
fi
if [[ -e "$launcher_file" ]]; then
  cp -a "$launcher_file" "$backup_dir/palomar"
  had_launcher=1
fi
if [[ -e "$unit_file" ]]; then
  cp -a "$unit_file" "$backup_dir/palomar.service"
  had_unit=1
fi
if [[ -e "$recovery_unit_file" ]]; then
  cp -a "$recovery_unit_file" "$backup_dir/palomar-update-recovery.service"
  had_recovery_unit=1
fi
if [[ -e "$helper_file" ]]; then
  cp -a "$helper_file" "$backup_dir/palomar-updater"
  had_helper=1
fi
mv -- "$staging_dir" "$install_dir"
staging_dir=""
rollback_required=1
install -m 755 "$project_dir/linux/palomar" "$launcher_file"
install -m 644 "$project_dir/linux/palomar.service" "$unit_file"
install -m 644 "$project_dir/linux/palomar-update-recovery.service" "$recovery_unit_file"
install -m 755 "$project_dir/linux/palomar_updater.py" "$helper_file"

python3 -m compileall -q \
  "$install_dir/palomar_service.py" \
  "$install_dir/codex.py" \
  "$install_dir/approvals.py" \
  "$install_dir/inputs.py" \
  "$install_dir/protocol.py" \
  "$install_dir/state.py" \
  "$install_dir/claude_code.py" \
  "$install_dir/session_identity.py" \
  "$install_dir/server_update.py" \
  "$install_dir/update_cli.py" \
  "$install_dir/vendor"
python3 "$install_dir/palomar_service.py" --help >/dev/null
systemctl --user daemon-reload
systemctl --user enable palomar.service
systemctl --user enable palomar-update-recovery.service
systemctl --user restart palomar.service
sleep 2
systemctl --user is-active --quiet palomar.service
rollback_required=0

install -d -m 755 "$bash_completion_dir" "$zsh_completion_dir" "$fish_completion_dir"
install -m 644 "$project_dir/linux/completions/palomar.bash" "$bash_completion_dir/palomar"
install -m 644 "$project_dir/linux/completions/_palomar" "$zsh_completion_dir/_palomar"
install -m 644 "$project_dir/linux/completions/palomar.fish" "$fish_completion_dir/palomar.fish"

rm -rf -- "$install_dir/venv"
rm -rf -- "$backup_dir"
backup_dir=""

echo
echo "Palomar is installed and running."
if [[ ":$PATH:" != *":$bin_dir:"* ]]; then
  echo "Add $bin_dir to PATH, then run:"
fi
echo "palomar pair"
