# Watch UI Specification

## States

- Starting
- No active navigation
- Active instruction
- Arriving
- Navigation finished
- Phone disconnected
- Stale instruction
- Protocol incompatible

## Active layout

- Large bitmap maneuver centered in upper region.
- Large formatted distance.
- Primary text: road or concise instruction.
- Secondary line only when space permits.
- Tiny phone/connection indicator.
- No map.

## Distance formatting

Phone sends meters. Watch formats using user/unit preference communicated by phone:
- metric: `850 m`, `1.2 km`;
- imperial future extension.
Initial release may be metric-only if explicitly shown in settings and protocol keeps room for unit flags.

## Vibration

Vibrate only when maneuver identity changes or an explicit threshold event is sent. Do not vibrate on every distance update.

## Stale data

If state age exceeds configured threshold, retain the instruction but visibly mark it stale. Never advance a maneuver locally based only on elapsed time.
