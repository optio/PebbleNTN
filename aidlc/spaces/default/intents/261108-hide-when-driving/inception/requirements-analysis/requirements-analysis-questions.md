# Requirements Analysis Questions: "When driving" (issue #132)

The behaviour, the settings and their names were agreed in the issue's "Proposed design" comment. These questions only cover what the design left open, as found in the code scan.

## Question 1
The driving speed has different ranges and defaults per unit (10–60 km/h with default 25, or 5–40 mph with default 15), and the watch's Distance units setting can change at any time. How should the chosen speed be stored so that a units change does not silently change its meaning?

A. Two speed settings, one metric and one imperial, each with its own default. The one that matches the current Distance units is shown and used (recommended: no conversion surprises, a units change simply switches to the other value)
B. One stored value in km/h. With imperial units the menus show it converted to the nearest 5 mph, and a choice in mph is stored back as the nearest km/h step
C. One stored list position. With imperial units the same position shows an mph label, so the range becomes 5–55 mph and the default would not be exactly 15 mph
X. Other (please specify)

[Answer]: A

## Question 2
An older watchapp (from before this feature) does not have the new settings. What should the phone app do while the connected watchapp is older?

A. Hide the "When driving" section in the phone app until the watchapp reports the new version; the watchapp updates itself automatically from the Pebble store (recommended: simplest, matches how other new watch settings were introduced)
B. Show the section, but disabled, with a line "Update the watchapp to use this"
C. Show and use it anyway, keeping the setting on the phone only for older watchapps
X. Other (please specify)

[Answer]: A

## Question 3
"Launch watchapp when navigation starts" can be turned off in the phone app. How should "Hide directions when driving" behave then?

A. Still close the watchapp when driving if it is open, but never reopen it automatically, because the user chose not to have it launched for them (recommended)
B. Close and reopen it just like with auto-launch on
C. Do nothing: hiding only applies when auto-launch is on
X. Other (please specify)

[Answer]: A

## Question 4
While directions are hidden for driving, should the phone app show that on its dashboard?

A. Yes, in the Status section: for example "Watch: hidden while driving (about 48 km/h)" (recommended: explains why the watch is quiet)
B. No, nothing changes on the phone
X. Other (please specify)

[Answer]: A

## Question 5
The diagnostics export will record the speed estimates and each hide or show decision (times and numbers only, no street names or notification text). In which export modes should that log be included?

A. Both the privacy-safe and the full export (recommended: it contains no notification text, and wrong guesses are most often reported with the privacy-safe export)
B. Only the full export
X. Other (please specify)

[Answer]: A
