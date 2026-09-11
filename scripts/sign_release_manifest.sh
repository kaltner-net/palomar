#!/usr/bin/env bash
set -euo pipefail

manifest="${1:?usage: sign_release_manifest.sh MANIFEST OUTPUT_DIRECTORY}"
output_directory="${2:?usage: sign_release_manifest.sh MANIFEST OUTPUT_DIRECTORY}"
: "${PALOMAR_ANDROID_KEYSTORE:?PALOMAR_ANDROID_KEYSTORE is required}"
: "${PALOMAR_ANDROID_KEYSTORE_PASSWORD:?PALOMAR_ANDROID_KEYSTORE_PASSWORD is required}"
: "${PALOMAR_ANDROID_KEY_ALIAS:?PALOMAR_ANDROID_KEY_ALIAS is required}"
: "${PALOMAR_ANDROID_KEY_PASSWORD:?PALOMAR_ANDROID_KEY_PASSWORD is required}"
export PALOMAR_ANDROID_KEYSTORE_PASSWORD PALOMAR_ANDROID_KEY_PASSWORD

temporary="$(mktemp -d)"
cleanup() { rm -rf -- "$temporary"; }
trap cleanup EXIT
chmod 700 "$temporary"

export PALOMAR_RELEASE_P12_PASSWORD
PALOMAR_RELEASE_P12_PASSWORD="$(openssl rand -hex 32)"
keytool -importkeystore -noprompt \
  -srckeystore "$PALOMAR_ANDROID_KEYSTORE" \
  -srcstorepass:env PALOMAR_ANDROID_KEYSTORE_PASSWORD \
  -srcalias "$PALOMAR_ANDROID_KEY_ALIAS" \
  -srckeypass:env PALOMAR_ANDROID_KEY_PASSWORD \
  -destkeystore "$temporary/release.p12" \
  -deststoretype PKCS12 \
  -deststorepass:env PALOMAR_RELEASE_P12_PASSWORD \
  -destkeypass:env PALOMAR_RELEASE_P12_PASSWORD
keytool -exportcert -rfc \
  -keystore "$PALOMAR_ANDROID_KEYSTORE" \
  -storepass:env PALOMAR_ANDROID_KEYSTORE_PASSWORD \
  -alias "$PALOMAR_ANDROID_KEY_ALIAS" \
  > "$output_directory/palomar-release-cert.pem"
openssl pkcs12 -in "$temporary/release.p12" -nocerts -nodes \
  -passin env:PALOMAR_RELEASE_P12_PASSWORD -out "$temporary/release-key.pem"
chmod 600 "$temporary/release-key.pem"
openssl dgst -sha256 -sign "$temporary/release-key.pem" \
  -out "$output_directory/palomar-SHA256SUMS.sig" "$manifest"
