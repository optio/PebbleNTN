# Requirements Analysis Questions: OsmAnd street and arrival time (#134)

Already decided in the issue and its decision comment, so not asked again:
- the second line shows the arrival time for Google Maps and OsmAnd, and German OsmAnd moves from remaining distance to arrival time;
- the road line comes from OsmAnd's expanded text (line 1) for every language, with the title as the fallback;
- per-language patterns are built from OsmAnd's own strings, not copied from German;
- the arrow and distance rules stay as they are.

## Q1. [Product] What should the road line show for OsmAnd?

OsmAnd's expanded line 1 reads `Sla rechtsaf en volg <Street> 215 m` (Dutch) or `rechts abbiegen, dann <Street> 400 m` (German). The arrow already shows the turn, and the road line holds 64 bytes, so the full sentence often gets cut. Whatever is chosen applies to every language, German included.

A. Street only (`<Street>`), with the instruction from the title as the fallback when there is no street (recommended: matches Google Maps, which shows the road name)
B. The full instruction with the street (`Sla rechtsaf en volg <Street>`), trailing distance removed
C. Street only, and the full instruction only for steps where OsmAnd names no turn street (continue steps, `Volg <Street>`)
X. Other (please specify)

[Answer]: X. Other: the full instruction (e.g. 'Sla rechtsaf en volg <Street>') stays, by default, with a setting for how compact or elaborate the road line is (changed at the requirements gate; see Q5–Q7) **Mode:** guided

## Q2. [Developer] How should the phone compute the arrival time it sends to the watch (`etaEpochSeconds`)?

Today no rule sends one. The watch already works out "time left" from the arrival clock text on the second line, so the main gain is the launcher countdown ("In 0:24 · ETA 11:00"), which needs the epoch, and an exact countdown in time-left mode.

A. A new arrival-clock extractor that turns `11:00` or `12:12 PM` into the next occurrence of that clock time, for OsmAnd and Google Maps (recommended: whole minutes, so an unchanged clock does not resend to the watch; one extractor for both apps; needs the protected rule schema, validator, Python workbench and rule spec updated)
B. Teach `DurationParser` OsmAnd's localized hour units (`1 u. 5 min`) and compute now + time left (smaller change, but the value moves every second, so every notification would be resent to the watch; OsmAnd only, as Google Maps shows no time left in its card)
C. Don't send an epoch; rely on the watch's clock-text countdown (no launcher countdown)
X. Other (please specify)

[Answer]: A. Arrival-clock extractor **Mode:** guided

## Q3. [Quality] How should the layout outside Dutch and German be checked?

Only the Dutch (private capture) and German (fixtures) layouts are confirmed from real notifications. The other languages would rely on OsmAnd's own strings.

A. Synthetic fixtures built from OsmAnd's strings for every language, plus OsmAnd on the Windows emulator with route-capture in English and French during Build and Test (recommended)
B. Synthetic fixtures from OsmAnd's strings only
C. Synthetic fixtures plus emulator runs in all 9 languages
X. Other (please specify)

[Answer]: A. Fixtures + emulator in English and French **Mode:** guided

## Q4. [Release] Should this PR also carry the Play debug-symbols fix (#136)?

#136 is a one-line build change (debug symbols in the Play bundle) that was parked to ship with the next feature, since every merge to main makes a release.

A. Yes, include #136 in this PR (recommended)
B. No, keep this PR to #134 only
X. Other (please specify)

[Answer]: A. Yes, include #136 **Mode:** guided

## Q5. [Product] Where should the compact/elaborate setting live?

Follow-up to the change requested at the requirements approval: the full instruction stays by default, with a setting to make the road line compact.

A. Watch and phone, kept in sync like the other watch settings (recommended)
B. Phone app only
X. Other (please specify)

[Answer]: A. Watch and phone **Mode:** guided

## Q6. [Product] How many levels should the setting have?

A. Two: Full (default, e.g. `Sla rechtsaf en volg Voorbeeldstraat`) and Compact (`Voorbeeldstraat`) (recommended)
B. Three: Full, Short (short turn word + street, per language), Street only
X. Other (please specify)

[Answer]: A. Two: Full / Compact **Mode:** guided

## Q7. [Product] Which apps should the setting cover in this PR?

A. OsmAnd now, with a general rule format so other apps can follow later (recommended)
B. All apps in this PR
X. Other (please specify)

[Answer]: B. All apps **Mode:** guided
