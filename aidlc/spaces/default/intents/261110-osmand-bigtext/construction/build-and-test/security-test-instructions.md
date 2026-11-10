# Security and Privacy Test Instructions: #134

There are no new permissions, network calls or stored personal data. The checks are:

- **NFR6, privacy:** no street, place, time or email from the private diagnostics exports appears in the change. Check:
  ```bash
  git diff main --stat
  ```
  Then grep the diff for the street names in the private captures (kept outside the repo, never printed). Fixtures must use made-up names such as `Voorbeeldstraat`, `Beispielweg` and `Example Street`, or public streets from route-capture runs.
- **Manifest unchanged:**
  ```bash
  git diff main -- android/app/src/main/AndroidManifest.xml
  ```
  This must print nothing.
- **Rule input safety:** the new `arrivalClock` extractor reads only a notification field and returns null on unreadable text (`ArrivalClockTest`). `RuleValidator` limits the pattern lengths of rule packs fetched from the remote.
