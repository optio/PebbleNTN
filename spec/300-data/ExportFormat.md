# Diagnostic and Rule Export Format

## Export modes

### Rules only — default
Contains selected user rules and metadata. No notification content.

### Privacy-safe diagnostic
Contains rules, extraction trace, package, locale, Android/app versions, and structurally redacted notification fields. The UI explains that potential private information is limited to content that was displayed by the source navigation app in its notification, but such content may include destinations, road names, saved-place labels and route context.

### Full diagnostic
Contains selected raw notification fields. Requires:
1. explicit selection;
2. privacy warning;
3. preview;
4. confirmation;
5. Android share sheet.

No export is sent automatically.

## Redaction

Privacy-safe export replaces:
- road names;
- destination labels;
- free-form text not required for rule matching;
- exact timestamps if user selects coarse-time mode.

It preserves structural tokens, units, punctuation and anonymized placeholders so maintainers can write rules.
