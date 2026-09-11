# Palomar product marks

Palomar uses the original **Dome Path** mark: a bold observatory arc over a
three-node control path. The mark can be read as an instrument looking outward
or as a control plane connecting persistent hosts. It is intentionally abstract
and is not based on Palomar Observatory or Caltech branding.

The geometry must remain recognizable without color at favicon and Android
status-bar sizes. Violet and cyan distinguish the instrument and control path
in full-color uses, but neither color nor a gradient is required for recognition.
Connection state is separate UI information and is never baked into the mark.

## Canonical family

| Variant | Source asset | Use |
| --- | --- | --- |
| Primary SVG mark | `web/public/palomar-mark.svg` | Web identity, favicon, PWA icon, and shared geometry reference |
| Horizontal wordmark | `docs/assets/palomar-wordmark.svg` | README and repository presentation |
| Repository preview | `docs/assets/palomar-social-preview.svg` | Source for the GitHub social preview |
| Android full-color vector | `android/app/src/main/res/drawable/palomar_mark.xml` | Splash, setup, pairing, headers, and About |
| Android adaptive foreground | `android/app/src/main/res/drawable/palomar_launcher_foreground.xml` | Launcher and round adaptive icons |
| Android monochrome | `android/app/src/main/res/drawable/palomar_launcher_monochrome.xml` | Android themed launcher icon |
| Notification silhouette | `android/app/src/main/res/drawable/ic_notification.xml` | Status-bar and notification small icon only |

Do not draw substitute letter avatars inside components. Reuse the platform
asset, preserve its aspect ratio and clear space, and keep presence/status dots
as separate semantic elements. The full-color mark uses `#171527`, `#D7CCFF`,
and `#7CE7E0`; monochrome variants use one platform-tinted solid silhouette.
