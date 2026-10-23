# Performance Test Instructions — Shortcuts (#12)

There are no load or latency targets. The resource check that matters is memory on aplite (24 KB):

- `pebble build`: aplite must keep a positive free heap at load. It is 3916 B with this work.
- In the emulator, open the deepest menu paths on aplite (Settings → Shortcuts → action list, Settings → About), and return to the navigation screen with no fault. Code Generation measured about 600–800 B free on these paths, after freeing cached bitmaps while the navigation window is covered.
- The banner uses one TextLayer and one AppTimer. Repeated actions reuse them, and nothing is allocated per action.
