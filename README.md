# Malimar TV Web — Shield Shell

Minimal Android TV WebView shell for:
https://hazerflac7.github.io/malimar-tv-web/

The Android app contains no Malimar catalog. The hosted Malimar TV Web remains the UI/source of catalog behavior.

Remote mapping:
- D-pad -> Arrow keys delivered to JavaScript
- Center/Enter -> Enter
- Back -> WebView history, then Android Back

Build with the included GitHub Actions workflow or `gradle :app:assembleDebug`.
