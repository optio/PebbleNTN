# Security Test Instructions — Prepare the first Google Play release (#114)

- **Signing material:** there is none in the repository. The upload key comes only from the `PLAY_UPLOAD_*` secrets in the protected `release` environment, and the workflow stops when any is missing (REQ-SEC-007). The temporary test key was deleted, and `git status` shows no keystore.
- **Release key isolation:** the release key stays in `release.yml` only. Play uploads use a separate upload key, which Google can reset if it leaks.
- **Play policy:** a Play install makes no GitHub request, ignores saved GitHub update data and points only to Google Play for updates. The app downloads no executable code.
- **Privacy statements:**
  - the privacy policy and the Data safety answers match the code's network use: the GitHub check on non-Play installs only, links the user opens, and email drafts the user sends;
  - online rule updates, which aren't implemented, are not claimed;
  - the onboarding disclosure has a Play variant.
